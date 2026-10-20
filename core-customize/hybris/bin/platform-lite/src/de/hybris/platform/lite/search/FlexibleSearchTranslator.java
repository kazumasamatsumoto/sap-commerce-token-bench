package de.hybris.platform.lite.search;

import de.hybris.bootstrap.typesystem.TypeSystem;
import de.hybris.bootstrap.typesystem.TypeSystem.AttributeDef;
import de.hybris.bootstrap.typesystem.TypeSystem.ItemTypeDef;
import de.hybris.bootstrap.typesystem.TypeSystem.RelationDef;
import de.hybris.platform.core.HybrisEnumValue;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.servicelayer.search.exceptions.FlexibleSearchException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * FlexibleSearch を PostgreSQL の SQL に読み替える。対応している書き方:
 * <ul>
 * <li>FROM {Product}、{Product AS p}、{Product! AS p}(子の型を含めない)、JOIN ... ON、LEFT JOIN</li>
 * <li>{code}、{p.code}、{p:code}、{p.name[ja]}、{pk}、{creationtime}、{modifiedtime}、{itemtype}</li>
 * <li>関連の型(多対多): {CategoryProductRelation AS r} と {r.source}、{r.target}</li>
 * <li>?code の名前付きの値、コレクションの値(IN (?codes))、{{ 副問い合わせ }}</li>
 * </ul>
 */
public class FlexibleSearchTranslator
{
	private static final Pattern FROM = Pattern.compile("(?i)\\bFROM\\s*\\{");
	private static final Pattern TYPE_REF = Pattern.compile("^\\s*(?:(LEFT|RIGHT|INNER)\\s+(?:OUTER\\s+)?)?(JOIN\\s+)?([A-Za-z0-9_]+)(!?)(?:\\s+AS\\s+([A-Za-z0-9_]+))?",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern ATTR = Pattern.compile("\\{\\s*(?:([A-Za-z0-9_]+)\\s*[.:]\\s*)?([A-Za-z0-9_]+)(?:\\[([A-Za-z_]+)\\])?(?::o)?\\s*\\}");
	private static final Pattern PARAM = Pattern.compile("\\?([A-Za-z_][A-Za-z0-9_.]*)");

	private final TypeSystem ts;

	public FlexibleSearchTranslator(final TypeSystem ts)
	{
		this.ts = ts;
	}

	/** 読み替えの結果。selectType は「SELECT {pk} だけ」のとき、その型(Model で返す)。 */
	public record Translated(String sql, List<Object> params, String selectType, boolean selectsOnlyPk)
	{
	}

	private static final class Alias
	{
		final String name;
		final ItemTypeDef type;
		final RelationDef relation;

		Alias(final String name, final ItemTypeDef type, final RelationDef relation)
		{
			this.name = name;
			this.type = type;
			this.relation = relation;
		}
	}

	public Translated translate(final String fsQuery, final Map<String, Object> params, final String language)
	{
		final Map<String, Alias> aliases = new LinkedHashMap<>();
		final String sql = translateOne(fsQuery, aliases, language);
		final List<Object> values = new ArrayList<>();
		final String finalSql = bindParams(sql, params, values);
		final Alias first = aliases.values().stream().findFirst().orElse(null);
		final boolean onlyPk = fsQuery.replaceAll("\\s+", " ").matches("(?is)^\\s*SELECT\\s+(DISTINCT\\s+)?\\{\\s*([A-Za-z0-9_]+\\s*[.:]\\s*)?pk\\s*\\}\\s+FROM.*");
		return new Translated(finalSql, values, first == null || first.type == null ? null : first.type.code, onlyPk);
	}

	private String translateSubqueries(final String q, final Map<String, Alias> outerAliases, final String language)
	{
		String s = q;
		int start;
		while ((start = s.indexOf("{{")) >= 0)
		{
			// {{ と { を区別して数え、対になる }} を探す({c.pk}} のように属性の } と FROM の } が続く形があるため)
			final java.util.ArrayDeque<Boolean> stack = new java.util.ArrayDeque<>(); // true = {{、false = {
			int end = -1;
			for (int i = start; i < s.length(); i++)
			{
				if (s.startsWith("{{", i))
				{
					stack.push(Boolean.TRUE);
					i++;
				}
				else if (s.charAt(i) == '{')
				{
					stack.push(Boolean.FALSE);
				}
				else if (s.charAt(i) == '}' && !stack.isEmpty())
				{
					if (!stack.peek())
					{
						stack.pop();
					}
					else if (s.startsWith("}}", i))
					{
						stack.pop();
						if (stack.isEmpty())
						{
							end = i;
							break;
						}
						i++;
					}
				}
			}
			if (end < 0)
			{
				throw new FlexibleSearchException("missing }} for subquery in: " + q);
			}
			final String inner = s.substring(start + 2, end);
			final Map<String, Alias> innerAliases = new LinkedHashMap<>(outerAliases);
			s = s.substring(0, start) + "(" + translateOne(inner, innerAliases, language) + ")" + s.substring(end + 2);
		}
		return s;
	}

	private String translateOne(final String q, final Map<String, Alias> aliases, final String language)
	{
		final Matcher m = FROM.matcher(q);
		if (!m.find())
		{
			throw new FlexibleSearchException("missing FROM {...} clause in query: " + q);
		}
		final int open = m.end() - 1;
		final int close = matchingBrace(q, open);
		final String fromBody = q.substring(open + 1, close);
		final List<String> typeConditions = new ArrayList<>();
		final String fromSql = translateFrom(fromBody, aliases, typeConditions, language);
		String head = q.substring(0, m.start());
		// 副問い合わせ {{ ... }} は外側の別名を使えるよう、外側の FROM を読んだあとで読み替える
		String tail = translateSubqueries(q.substring(close + 1), aliases, language);
		head = translateAttributes(head, aliases, language);
		tail = translateAttributes(tail, aliases, language);
		if (!typeConditions.isEmpty())
		{
			tail = injectWhere(tail, String.join(" AND ", typeConditions));
		}
		return head + "FROM " + fromSql + tail;
	}

	private String translateFrom(final String body, final Map<String, Alias> aliases, final List<String> typeConditions, final String language)
	{
		// JOIN の区切りで分ける。ON の中の {..} は後で読み替える。
		final List<String> parts = splitJoins(body);
		final StringBuilder sql = new StringBuilder();
		int index = aliases.size();
		for (final String part : parts)
		{
			final Matcher m = TYPE_REF.matcher(part);
			if (!m.find())
			{
				throw new FlexibleSearchException("cannot parse FROM part '" + part.trim() + "'");
			}
			final String joinKind = m.group(1);
			final boolean isJoin = m.group(2) != null;
			final String typeCode = m.group(3);
			final boolean exact = "!".equals(m.group(4));
			final String aliasName = m.group(5) != null ? m.group(5) : "item_t" + index;
			index++;
			String rest = part.substring(m.end()).trim();
			final Alias alias = resolveAlias(aliasName, typeCode);
			aliases.put(aliasName.toLowerCase(Locale.ROOT), alias);
			final String table = alias.relation != null ? alias.relation.table : ts.tableOf(alias.type);
			final String typeCondition = alias.relation != null ? null : typeCondition(aliasName, alias.type, exact);
			if (isJoin)
			{
				sql.append(' ').append(joinKind == null ? "" : joinKind.toUpperCase(Locale.ROOT) + " ").append("JOIN ").append(table).append(' ')
						.append(aliasName);
				if (rest.toUpperCase(Locale.ROOT).startsWith("ON"))
				{
					rest = rest.substring(2);
				}
				final String on = translateAttributes(rest, aliases, language);
				sql.append(" ON (").append(on.isBlank() ? "1=1" : on).append(')');
				if (typeCondition != null)
				{
					sql.append(" AND ").append(typeCondition);
				}
			}
			else
			{
				if (sql.length() > 0)
				{
					sql.append(", ");
				}
				sql.append(table).append(' ').append(aliasName);
				if (typeCondition != null)
				{
					typeConditions.add(typeCondition);
				}
			}
		}
		return sql.toString();
	}

	private Alias resolveAlias(final String aliasName, final String typeCode)
	{
		final var relation = ts.findRelation(typeCode);
		if (relation.isPresent() && relation.get().manyToMany())
		{
			return new Alias(aliasName, null, relation.get());
		}
		final ItemTypeDef type = ts.findItemType(typeCode)
				.orElseThrow(() -> new FlexibleSearchException("type code '" + typeCode + "' invalid"));
		return new Alias(aliasName, type, null);
	}

	private String typeCondition(final String alias, final ItemTypeDef type, final boolean exact)
	{
		final List<ItemTypeDef> types = exact ? List.of(type) : ts.withSubtypes(type);
		return alias + ".typecode IN (" + types.stream().map(t -> "'" + t.code + "'").collect(Collectors.joining(",")) + ")";
	}

	private static List<String> splitJoins(final String body)
	{
		final List<String> parts = new ArrayList<>();
		final Matcher m = Pattern.compile("(?i)\\s((?:LEFT|RIGHT|INNER)\\s+(?:OUTER\\s+)?)?JOIN\\s").matcher(body);
		int last = 0;
		int depth = 0;
		final List<Integer> cuts = new ArrayList<>();
		while (m.find())
		{
			depth = braceDepth(body, m.start());
			if (depth == 0)
			{
				cuts.add(m.start());
			}
		}
		for (final int cut : cuts)
		{
			parts.add(body.substring(last, cut));
			last = cut;
		}
		parts.add(body.substring(last));
		return parts;
	}

	private static int braceDepth(final String s, final int upTo)
	{
		int d = 0;
		for (int i = 0; i < upTo; i++)
		{
			if (s.charAt(i) == '{')
			{
				d++;
			}
			else if (s.charAt(i) == '}')
			{
				d--;
			}
		}
		return d;
	}

	private static int matchingBrace(final String s, final int open)
	{
		int depth = 0;
		for (int i = open; i < s.length(); i++)
		{
			if (s.charAt(i) == '{')
			{
				depth++;
			}
			else if (s.charAt(i) == '}')
			{
				depth--;
				if (depth == 0)
				{
					return i;
				}
			}
		}
		throw new FlexibleSearchException("missing } in FROM clause: " + s);
	}

	private String translateAttributes(final String s, final Map<String, Alias> aliases, final String language)
	{
		final Matcher m = ATTR.matcher(s);
		final StringBuilder out = new StringBuilder();
		while (m.find())
		{
			m.appendReplacement(out, Matcher.quoteReplacement(column(m.group(1), m.group(2), m.group(3), aliases, language)));
		}
		m.appendTail(out);
		return out.toString();
	}

	private String column(final String aliasName, final String qualifier, final String lang, final Map<String, Alias> aliases,
			final String language)
	{
		final Alias alias;
		if (aliasName == null)
		{
			if (aliases.size() != 1 && aliases.values().stream().filter(a -> a.type != null).count() != 1)
			{
				throw new FlexibleSearchException("ambiguous attribute {" + qualifier + "} - use {alias." + qualifier + "} when the query has more than one type");
			}
			alias = aliases.values().stream().filter(a -> a.type != null).findFirst().orElse(aliases.values().iterator().next());
		}
		else
		{
			alias = aliases.get(aliasName.toLowerCase(Locale.ROOT));
			if (alias == null)
			{
				throw new FlexibleSearchException("unknown alias '" + aliasName + "' in {" + aliasName + "." + qualifier + "}");
			}
		}
		final String q = qualifier.toLowerCase(Locale.ROOT);
		if (alias.relation != null)
		{
			return switch (q)
			{
				case "source", "pk" -> alias.name + ".sourcepk";
				case "target" -> alias.name + ".targetpk";
				case "sequencenumber" -> alias.name + ".sequencenumber";
				case "reversesequencenumber" -> alias.name + ".reversesequencenumber";
				default -> throw new FlexibleSearchException("unknown attribute '" + qualifier + "' of relation " + alias.relation.code);
			};
		}
		switch (q)
		{
			case "pk":
				return alias.name + ".pk";
			case "creationtime":
				return alias.name + ".createdts";
			case "modifiedtime":
				return alias.name + ".modifiedts";
			case "itemtype":
			case "typecode":
				return alias.name + ".typecode";
			default:
				break;
		}
		final AttributeDef a = findInTypeOrSubtypes(alias.type, qualifier);
		if (!ts.hasColumn(a))
		{
			throw new FlexibleSearchException("cannot search attribute '" + qualifier + "' of type " + alias.type.code
					+ " - it is a collection or a dynamic attribute and has no column (use a JOIN on the relation instead)");
		}
		final String col = alias.name + "." + TypeSystem.columnName(a.qualifier);
		if (a.isLocalized())
		{
			return "(" + col + " ->> '" + (lang != null ? lang : language) + "')";
		}
		if (lang != null)
		{
			throw new FlexibleSearchException("attribute '" + qualifier + "' of type " + alias.type.code + " is not localized - remove [" + lang + "]");
		}
		return col;
	}

	private AttributeDef findInTypeOrSubtypes(final ItemTypeDef type, final String qualifier)
	{
		for (final ItemTypeDef t : ts.withSubtypes(type))
		{
			final var a = ts.findAttribute(t, qualifier);
			if (a.isPresent())
			{
				return a.get();
			}
		}
		throw new FlexibleSearchException("cannot search unknown field 'TableField(name='" + qualifier + "',langPK='null',type=" + type.code
				+ ")' within type " + type.code + " unless you disable checking");
	}

	private static String injectWhere(final String tail, final String condition)
	{
		final Matcher where = Pattern.compile("(?i)\\bWHERE\\b").matcher(tail);
		if (where.find() && braceDepth(tail, where.start()) == 0 && parenDepth(tail, where.start()) == 0)
		{
			return tail.substring(0, where.end()) + " " + condition + " AND (" + closeWhere(tail.substring(where.end()));
		}
		final Matcher clause = Pattern.compile("(?i)\\b(GROUP\\s+BY|ORDER\\s+BY|HAVING|LIMIT|UNION)\\b").matcher(tail);
		while (clause.find())
		{
			if (parenDepth(tail, clause.start()) == 0)
			{
				return tail.substring(0, clause.start()) + " WHERE " + condition + " " + tail.substring(clause.start());
			}
		}
		return tail + " WHERE " + condition;
	}

	/** WHERE の後ろを (...) で包む。GROUP BY などが来たらそこで閉じる。 */
	private static String closeWhere(final String afterWhere)
	{
		final Matcher clause = Pattern.compile("(?i)\\b(GROUP\\s+BY|ORDER\\s+BY|HAVING|LIMIT|UNION)\\b").matcher(afterWhere);
		while (clause.find())
		{
			if (parenDepth(afterWhere, clause.start()) == 0)
			{
				return afterWhere.substring(0, clause.start()) + ") " + afterWhere.substring(clause.start());
			}
		}
		return afterWhere + ")";
	}

	private static int parenDepth(final String s, final int upTo)
	{
		int d = 0;
		boolean inString = false;
		for (int i = 0; i < upTo; i++)
		{
			final char c = s.charAt(i);
			if (c == '\'')
			{
				inString = !inString;
			}
			else if (!inString && c == '(')
			{
				d++;
			}
			else if (!inString && c == ')')
			{
				d--;
			}
		}
		return d;
	}

	private static String bindParams(final String sql, final Map<String, Object> params, final List<Object> values)
	{
		final Matcher m = PARAM.matcher(sql);
		final StringBuilder out = new StringBuilder();
		while (m.find())
		{
			final String name = m.group(1);
			if (!params.containsKey(name))
			{
				throw new FlexibleSearchException("missing value for query parameter ?" + name);
			}
			final Object v = params.get(name);
			if (v instanceof Collection<?> c)
			{
				if (c.isEmpty())
				{
					m.appendReplacement(out, "NULL");
				}
				else
				{
					m.appendReplacement(out, c.stream().map(x -> "?").collect(Collectors.joining(",")));
					c.forEach(x -> values.add(toSqlValue(x)));
				}
			}
			else
			{
				m.appendReplacement(out, "?");
				values.add(toSqlValue(v));
			}
		}
		m.appendTail(out);
		return out.toString();
	}

	private static Object toSqlValue(final Object v)
	{
		if (v instanceof ItemModel m)
		{
			if (m.getPk() == null)
			{
				throw new FlexibleSearchException("cannot use unsaved model " + m + " as query parameter");
			}
			return m.getPk().getLongValue();
		}
		if (v instanceof PK pk)
		{
			return pk.getLongValue();
		}
		if (v instanceof HybrisEnumValue e)
		{
			return e.getCode();
		}
		if (v instanceof Date d)
		{
			return new Timestamp(d.getTime());
		}
		return v;
	}
}
