package de.hybris.platform.lite.impex;

import de.hybris.bootstrap.typesystem.TypeSystem;
import de.hybris.bootstrap.typesystem.TypeSystem.AttributeDef;
import de.hybris.bootstrap.typesystem.TypeSystem.ItemTypeDef;
import de.hybris.bootstrap.typesystem.TypeSystem.Kind;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.lite.persistence.ItemPersistence;
import de.hybris.platform.servicelayer.exceptions.SystemException;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ImpEx を読み込む。本物の書き方のうち、よく使うものに対応する:
 * <ul>
 * <li>INSERT / INSERT_UPDATE / UPDATE / REMOVE のヘッダーと、; で区切った値の行(先頭の列は型の上書き)</li>
 * <li>$macro=値 のマクロ、# のコメント、行末の \ で次の行に続ける</li>
 * <li>[unique=true] [lang=ja] [default=...] [mode=append] [dateformat=...] [allownull=true] [virtual=true]</li>
 * <li>参照の解決 catalogVersion(catalog(id),version) と、値の ':' 区切り。コレクションは ',' 区切り</li>
 * <li>解決できない行は後回しにして何周か試し、最後まで残ったら「unresolved lines」としてエラーにする</li>
 * </ul>
 * BeanShell(#%)、translator、processor には対応しない(警告を出して飛ばす)。
 */
public class ImpExImporter
{
	private static final Logger LOG = LoggerFactory.getLogger(ImpExImporter.class);
	private static final Pattern HEADER = Pattern.compile("^\\s*(INSERT_UPDATE|INSERT|UPDATE|REMOVE)\\s+([A-Za-z0-9_]+)(\\[[^]]*\\])?\\s*(;.*)?$",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern MACRO_DEF = Pattern.compile("^\\s*(\\$[A-Za-z0-9_\\-.]+)\\s*=(.*)$");
	private static final int MAX_PASSES = 5;

	private final ModelService modelService;
	private final FlexibleSearchService flexibleSearchService;
	private final TypeSystem ts;

	public ImpExImporter(final ModelService modelService, final FlexibleSearchService flexibleSearchService, final ItemPersistence persistence)
	{
		this.modelService = modelService;
		this.flexibleSearchService = flexibleSearchService;
		this.ts = persistence.getTypeSystem();
	}

	/** 結果。unresolved が空なら成功。 */
	public record Result(String name, int imported, List<String> unresolved, List<String> warnings)
	{
		public boolean isSuccessful()
		{
			return unresolved.isEmpty();
		}
	}

	private record Line(int number, String text, Header header)
	{
	}

	// ------------------------------------------------------------------ 読む

	public Result importScript(final String name, final String script)
	{
		final List<String> warnings = new ArrayList<>();
		final Map<String, String> macros = new LinkedHashMap<>();
		final List<Line> lines = new ArrayList<>();
		Header header = null;
		final String[] raw = joinContinuations(script.replace("\r\n", "\n").replace('\r', '\n')).split("\n", -1);
		for (int i = 0; i < raw.length; i++)
		{
			final String text = raw[i];
			final int lineNo = i + 1;
			final String trimmed = text.trim();
			if (trimmed.isEmpty())
			{
				continue;
			}
			if (trimmed.startsWith("#%"))
			{
				warnings.add(name + " line " + lineNo + ": BeanShell (#%) is not supported and was skipped");
				continue;
			}
			if (trimmed.startsWith("#"))
			{
				continue;
			}
			final Matcher md = MACRO_DEF.matcher(text);
			if (md.matches() && !trimmed.startsWith(";"))
			{
				macros.put(md.group(1), applyMacros(md.group(2).trim(), macros));
				continue;
			}
			final String expanded = applyMacros(text, macros);
			final Matcher hm = HEADER.matcher(expanded);
			if (hm.matches())
			{
				header = parseHeader(name, lineNo, hm, warnings);
				continue;
			}
			if (expanded.trim().startsWith(";") || header != null && !expanded.trim().isEmpty())
			{
				if (header == null)
				{
					throw new ImpExException(name + " line " + lineNo + ": value line without a header: " + text);
				}
				lines.add(new Line(lineNo, expanded, header));
			}
		}

		int imported = 0;
		List<Line> pending = lines;
		final Map<Line, String> reasons = new LinkedHashMap<>();
		for (int pass = 1; pass <= MAX_PASSES && !pending.isEmpty(); pass++)
		{
			final List<Line> next = new ArrayList<>();
			for (final Line line : pending)
			{
				try
				{
					processLine(line);
					imported++;
					reasons.remove(line);
				}
				catch (final UnresolvedException | SystemException | IllegalArgumentException e)
				{
					reasons.put(line, e.getMessage());
					next.add(line);
				}
			}
			if (next.size() == pending.size())
			{
				pending = next;
				break;
			}
			pending = next;
		}
		final List<String> unresolved = new ArrayList<>();
		for (final Line l : pending)
		{
			unresolved.add("line " + l.number() + " at " + name + ": " + reasons.get(l) + System.lineSeparator() + "    " + l.text().trim());
		}
		return new Result(name, imported, unresolved, warnings);
	}

	private static String joinContinuations(final String s)
	{
		// 行末の \ は次の行に続ける。行番号がずれないよう、続けた分だけ空行を足す。
		final StringBuilder out = new StringBuilder();
		int pendingBlank = 0;
		for (final String line : s.split("\n", -1))
		{
			if (line.endsWith("\\"))
			{
				out.append(line, 0, line.length() - 1);
				pendingBlank++;
			}
			else
			{
				out.append(line).append('\n');
				out.append("\n".repeat(pendingBlank));
				pendingBlank = 0;
			}
		}
		return out.toString();
	}

	private static String applyMacros(final String text, final Map<String, String> macros)
	{
		String s = text;
		final List<String> names = new ArrayList<>(macros.keySet());
		names.sort(Comparator.comparingInt(String::length).reversed());
		for (final String n : names)
		{
			s = s.replace(n, macros.get(n));
		}
		return s;
	}

	// ------------------------------------------------------------------ ヘッダー

	private static final class Header
	{
		String mode;
		ItemTypeDef type;
		final List<Column> columns = new ArrayList<>();
	}

	private static final class Column
	{
		String qualifier;
		Spec spec;
		final Map<String, String> modifiers = new HashMap<>();
		AttributeDef attribute;
		boolean skip;

		boolean unique()
		{
			return "true".equalsIgnoreCase(modifiers.get("unique"));
		}
	}

	/** 参照の書き方 catalogVersion(catalog(id),version) の (...) の中。 */
	private record Spec(String qualifier, List<Spec> children)
	{
		int leafCount()
		{
			return children.isEmpty() ? 1 : children.stream().mapToInt(Spec::leafCount).sum();
		}
	}

	private Header parseHeader(final String name, final int lineNo, final Matcher hm, final List<String> warnings)
	{
		final Header h = new Header();
		h.mode = hm.group(1).toUpperCase(Locale.ROOT);
		h.type = ts.findItemType(hm.group(2)).orElseThrow(() -> new ImpExException(
				name + " line " + lineNo + ": unknown type '" + hm.group(2) + "' in header (check the type code in *-items.xml)"));
		final String cols = hm.group(4) == null ? "" : hm.group(4).substring(1);
		for (final String rawCol : splitCells(cols))
		{
			final Column c = parseColumn(rawCol.trim());
			if (c.qualifier.isEmpty())
			{
				c.skip = true;
			}
			else if (c.qualifier.startsWith("@") || c.modifiers.containsKey("translator") || c.modifiers.containsKey("processor"))
			{
				warnings.add(name + " line " + lineNo + ": column '" + rawCol.trim() + "' uses a translator and was skipped");
				c.skip = true;
			}
			else if ("true".equalsIgnoreCase(c.modifiers.get("virtual")))
			{
				c.skip = true;
			}
			else
			{
				c.attribute = findAttribute(h.type, c.qualifier).orElseThrow(() -> new ImpExException(name + " line " + lineNo
						+ ": unknown attribute '" + c.qualifier + "' in header for type " + h.type.code));
			}
			h.columns.add(c);
		}
		return h;
	}

	private java.util.Optional<AttributeDef> findAttribute(final ItemTypeDef type, final String qualifier)
	{
		for (final ItemTypeDef t : ts.withSubtypes(type))
		{
			final var a = ts.findAttribute(t, qualifier);
			if (a.isPresent())
			{
				return a;
			}
		}
		return java.util.Optional.empty();
	}

	private static Column parseColumn(final String raw)
	{
		final Column c = new Column();
		String s = raw;
		// 末尾の [..][..] を外す(括弧の外にあるものだけ)
		while (s.endsWith("]"))
		{
			final int open = s.lastIndexOf('[');
			for (final String mod : s.substring(open + 1, s.length() - 1).split(","))
			{
				final int eq = mod.indexOf('=');
				if (eq > 0)
				{
					c.modifiers.put(mod.substring(0, eq).trim().toLowerCase(Locale.ROOT), unquote(mod.substring(eq + 1).trim()));
				}
			}
			s = s.substring(0, open).trim();
		}
		final int paren = s.indexOf('(');
		if (paren < 0)
		{
			c.qualifier = s.trim();
			c.spec = null;
		}
		else
		{
			c.qualifier = s.substring(0, paren).trim();
			c.spec = parseSpec(c.qualifier, s.substring(paren + 1, s.lastIndexOf(')')));
		}
		return c;
	}

	private static String unquote(final String s)
	{
		return s.length() >= 2 && (s.startsWith("'") && s.endsWith("'") || s.startsWith("\"") && s.endsWith("\"")) ? s.substring(1, s.length() - 1) : s;
	}

	private static Spec parseSpec(final String qualifier, final String inner)
	{
		final List<Spec> children = new ArrayList<>();
		int depth = 0;
		int start = 0;
		for (int i = 0; i <= inner.length(); i++)
		{
			final char ch = i < inner.length() ? inner.charAt(i) : ',';
			if (ch == '(')
			{
				depth++;
			}
			else if (ch == ')')
			{
				depth--;
			}
			else if (ch == ',' && depth == 0)
			{
				final String part = inner.substring(start, i).trim();
				if (!part.isEmpty())
				{
					final int p = part.indexOf('(');
					children.add(p < 0 ? new Spec(part.replaceAll("\\[.*\\]$", ""), List.of())
							: parseSpec(part.substring(0, p).trim(), part.substring(p + 1, part.lastIndexOf(')'))));
				}
				start = i + 1;
			}
		}
		return new Spec(qualifier, children);
	}

	// ------------------------------------------------------------------ 値の行

	private static final class UnresolvedException extends RuntimeException
	{
		UnresolvedException(final String message)
		{
			super(message);
		}
	}

	private void processLine(final Line line)
	{
		final Header h = line.header();
		final List<String> cells = splitCells(line.text().trim().startsWith(";") ? line.text().trim().substring(1) : line.text().trim());
		ItemTypeDef type = h.type;
		final String first = line.text().trim().startsWith(";") ? "" : cells.remove(0);
		if (!first.isBlank())
		{
			type = ts.findItemType(first.trim()).orElseThrow(() -> new UnresolvedException("unknown type '" + first.trim() + "' in first column"));
		}
		final Map<Column, Object> values = new LinkedHashMap<>();
		final Map<Column, Object> uniqueValues = new LinkedHashMap<>();
		for (int i = 0; i < h.columns.size(); i++)
		{
			final Column c = h.columns.get(i);
			if (c.skip)
			{
				continue;
			}
			String cell = i < cells.size() ? cells.get(i) : "";
			if (cell.isEmpty() && c.modifiers.containsKey("default"))
			{
				cell = c.modifiers.get("default");
			}
			if (cell.isEmpty())
			{
				if (c.unique() && !"REMOVE".equals(h.mode))
				{
					throw new UnresolvedException("value is NULL for mandatory unique attribute '" + c.qualifier + "'");
				}
				continue;
			}
			final Object v = "<null>".equalsIgnoreCase(cell) ? null : convert(c, cell);
			values.put(c, v);
			if (c.unique())
			{
				uniqueValues.put(c, v);
			}
		}

		ItemModel existing = null;
		if (!"INSERT".equals(h.mode))
		{
			if (uniqueValues.isEmpty())
			{
				throw new ImpExException("header for " + h.type.code + " (" + h.mode + ") has no [unique=true] column");
			}
			final List<ItemModel> found = findByUnique(type, uniqueValues);
			if (found.size() > 1)
			{
				throw new UnresolvedException("found " + found.size() + " items for unique key " + display(uniqueValues));
			}
			existing = found.isEmpty() ? null : found.get(0);
		}
		switch (h.mode)
		{
			case "REMOVE":
				if (existing != null)
				{
					modelService.remove(existing);
				}
				return;
			case "UPDATE":
				if (existing == null)
				{
					throw new UnresolvedException("no existing item found for update with unique key " + display(uniqueValues));
				}
				break;
			default:
				break;
		}
		final ItemModel model = existing != null ? existing : modelService.create(type.code);
		for (final Map.Entry<Column, Object> e : values.entrySet())
		{
			setValue(model, e.getKey(), e.getValue());
		}
		modelService.save(model);
	}

	@SuppressWarnings("unchecked")
	private void setValue(final ItemModel model, final Column c, final Object value)
	{
		final AttributeDef a = c.attribute;
		if (a.isLocalized())
		{
			final String lang = c.modifiers.getOrDefault("lang", "ja");
			invoke(model, "set" + cap(a.qualifier), new Class<?>[] { String.class, Locale.class }, value, Locale.forLanguageTag(lang));
			return;
		}
		if (ts.kindOf(a) == Kind.COLLECTION && "append".equalsIgnoreCase(c.modifiers.get("mode")) && value instanceof Collection<?> add)
		{
			final Collection<Object> merged = new LinkedHashSet<>((Collection<Object>) modelService.getAttributeValue(model, a.qualifier));
			merged.addAll(add);
			modelService.setAttributeValue(model, a.qualifier, new ArrayList<>(merged));
			return;
		}
		modelService.setAttributeValue(model, a.qualifier, value);
	}

	private Object convert(final Column c, final String cell)
	{
		final AttributeDef a = c.attribute;
		final Kind kind = ts.kindOf(a);
		try
		{
			switch (kind)
			{
				case STRING:
				case LOCALIZED_STRING:
					return cell;
				case INTEGER:
					return Integer.valueOf(cell.trim());
				case LONG:
					return Long.valueOf(cell.trim());
				case DOUBLE:
					return Double.valueOf(cell.trim());
				case DECIMAL:
					return new BigDecimal(cell.trim());
				case BOOLEAN:
					if (!cell.trim().matches("(?i)true|false"))
					{
						throw new IllegalArgumentException("'" + cell + "' is not a boolean (true / false)");
					}
					return Boolean.valueOf(cell.trim());
				case DATE:
					return parseDate(cell.trim(), c.modifiers.get("dateformat"));
				case ENUM:
					return ItemPersistence.enumValue(a.type, cell.trim());
				case ITEM:
					return resolveReference(a.type, c.spec, c.qualifier, cell.trim());
				case COLLECTION:
				{
					final List<Object> out = new ArrayList<>();
					for (final String part : cell.split(","))
					{
						if (part.isBlank())
						{
							continue;
						}
						if (a.relation != null)
						{
							out.add(resolveReference(a.type, c.spec, c.qualifier, part.trim()));
						}
						else
						{
							final String element = ts.findCollectionType(a.type).map(TypeSystem.CollectionTypeDef::elementType).orElse("java.lang.String");
							out.add(ts.findEnumType(element).isPresent() ? ItemPersistence.enumValue(element, part.trim()) : part.trim());
						}
					}
					return out;
				}
				default:
					throw new IllegalArgumentException("unsupported attribute type " + a.type);
			}
		}
		catch (final NumberFormatException e)
		{
			throw new UnresolvedException("cannot convert '" + cell + "' for attribute '" + a.qualifier + "' (" + a.type + ")");
		}
	}

	private static Object parseDate(final String s, final String format)
	{
		for (final String f : format != null ? List.of(format) : List.of("dd.MM.yyyy HH:mm:ss", "dd.MM.yyyy", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd"))
		{
			try
			{
				final SimpleDateFormat sdf = new SimpleDateFormat(f);
				sdf.setLenient(false);
				return sdf.parse(s);
			}
			catch (final ParseException ignored)
			{
				// 次の形を試す
			}
		}
		throw new UnresolvedException("cannot parse date '" + s + "'" + (format == null ? " (use dd.MM.yyyy HH:mm:ss or [dateformat=...])" : " with format " + format));
	}

	/** 参照を探す。spec が無いときは code か uid か isocode で探す。 */
	private ItemModel resolveReference(final String typeCode, final Spec spec, final String qualifier, final String value)
	{
		final ItemTypeDef type = ts.getItemType(typeCode);
		final Spec effective = spec != null ? spec : new Spec(qualifier, List.of(new Spec(defaultKey(type), List.of())));
		final List<String> parts = splitColon(value);
		if (parts.size() != effective.leafCount())
		{
			throw new UnresolvedException("cannot resolve value '" + value + "' for attribute '" + qualifier + "': expected " + effective.leafCount()
					+ " part(s) separated by ':' for " + describe(effective) + " but got " + parts.size());
		}
		final ItemModel found = resolve(type, effective, parts.iterator());
		if (found == null)
		{
			throw new UnresolvedException("cannot resolve value '" + value + "' for attribute '" + qualifier + "' (" + typeCode + describe(effective)
					+ ") - no such item (yet)");
		}
		return found;
	}

	private ItemModel resolve(final ItemTypeDef type, final Spec spec, final java.util.Iterator<String> parts)
	{
		final StringBuilder q = new StringBuilder("SELECT {pk} FROM {" + type.code + "}");
		final Map<String, Object> params = new HashMap<>();
		String sep = " WHERE ";
		int i = 0;
		for (final Spec child : spec.children())
		{
			final AttributeDef a = findAttribute(type, child.qualifier())
					.orElseThrow(() -> new UnresolvedException("unknown attribute '" + child.qualifier() + "' of type " + type.code + " in reference"));
			final Object v;
			if (ts.kindOf(a) == Kind.ITEM)
			{
				final Spec sub = child.children().isEmpty() ? new Spec(child.qualifier(), List.of(new Spec(defaultKey(ts.getItemType(a.type)), List.of()))) : child;
				v = resolve(ts.getItemType(a.type), sub, parts);
				if (v == null)
				{
					return null;
				}
			}
			else
			{
				final String raw = parts.next();
				v = ts.kindOf(a) == Kind.ENUM ? ItemPersistence.enumValue(a.type, raw) : raw;
			}
			final String p = "p" + i++;
			q.append(sep).append('{').append(a.qualifier).append(a.isLocalized() ? "[ja]" : "").append("} = ?").append(p);
			params.put(p, v);
			sep = " AND ";
		}
		final List<ItemModel> result = flexibleSearchService.<ItemModel> search(q.toString(), params).getResult();
		if (result.size() > 1)
		{
			throw new UnresolvedException("ambiguous reference: " + result.size() + " items of type " + type.code + " match " + describe(spec));
		}
		return result.isEmpty() ? null : result.get(0);
	}

	private String defaultKey(final ItemTypeDef type)
	{
		for (final String k : List.of("code", "uid", "isocode", "id"))
		{
			if (ts.findAttribute(type, k).isPresent())
			{
				return k;
			}
		}
		throw new UnresolvedException("type " + type.code + " needs an explicit reference pattern like " + type.code + "(code)");
	}

	private List<ItemModel> findByUnique(final ItemTypeDef type, final Map<Column, Object> uniqueValues)
	{
		final StringBuilder q = new StringBuilder("SELECT {pk} FROM {" + type.code + "}");
		final Map<String, Object> params = new HashMap<>();
		String sep = " WHERE ";
		int i = 0;
		for (final Map.Entry<Column, Object> e : uniqueValues.entrySet())
		{
			final AttributeDef a = e.getKey().attribute;
			final String p = "u" + i++;
			if (a.isLocalized())
			{
				q.append(sep).append('{').append(a.qualifier).append('[').append(e.getKey().modifiers.getOrDefault("lang", "ja")).append("]} = ?").append(p);
			}
			else
			{
				q.append(sep).append('{').append(a.qualifier).append("} = ?").append(p);
			}
			params.put(p, e.getValue());
			sep = " AND ";
		}
		return flexibleSearchService.<ItemModel> search(q.toString(), params).getResult();
	}

	// ------------------------------------------------------------------ 道具

	private static List<String> splitCells(final String s)
	{
		final List<String> out = new ArrayList<>();
		final StringBuilder cur = new StringBuilder();
		boolean quoted = false;
		for (int i = 0; i < s.length(); i++)
		{
			final char ch = s.charAt(i);
			if (ch == '"')
			{
				if (quoted && i + 1 < s.length() && s.charAt(i + 1) == '"')
				{
					cur.append('"');
					i++;
				}
				else
				{
					quoted = !quoted;
				}
			}
			else if (ch == ';' && !quoted)
			{
				out.add(cur.toString().trim());
				cur.setLength(0);
			}
			else
			{
				cur.append(ch);
			}
		}
		out.add(cur.toString().trim());
		return out;
	}

	private static List<String> splitColon(final String s)
	{
		final List<String> out = new ArrayList<>();
		for (final String p : s.split("(?<!\\\\):", -1))
		{
			out.add(p.replace("\\:", ":").trim());
		}
		return out;
	}

	private static String describe(final Spec spec)
	{
		if (spec.children().isEmpty())
		{
			return spec.qualifier();
		}
		final StringBuilder b = new StringBuilder("(");
		for (int i = 0; i < spec.children().size(); i++)
		{
			b.append(i > 0 ? "," : "").append(describe(spec.children().get(i)));
		}
		return b.append(')').toString();
	}

	private static String display(final Map<Column, Object> values)
	{
		final StringBuilder b = new StringBuilder("{");
		values.forEach((c, v) -> b.append(b.length() > 1 ? ", " : "").append(c.qualifier).append('=').append(v));
		return b.append('}').toString();
	}

	private static String cap(final String s)
	{
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	private static void invoke(final Object target, final String method, final Class<?>[] types, final Object... args)
	{
		try
		{
			final Method m = target.getClass().getMethod(method, types);
			m.invoke(target, args);
		}
		catch (final ReflectiveOperationException e)
		{
			throw new IllegalStateException("cannot call " + method + " on " + target.getClass().getSimpleName(), e);
		}
	}
}
