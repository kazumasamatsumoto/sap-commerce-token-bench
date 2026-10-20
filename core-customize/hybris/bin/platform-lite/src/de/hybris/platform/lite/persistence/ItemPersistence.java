package de.hybris.platform.lite.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hybris.bootstrap.typesystem.TypeSystem;
import de.hybris.bootstrap.typesystem.TypeSystem.AttributeDef;
import de.hybris.bootstrap.typesystem.TypeSystem.ItemTypeDef;
import de.hybris.bootstrap.typesystem.TypeSystem.Kind;
import de.hybris.platform.core.HybrisEnumValue;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.lite.runtime.TypeSystemHolder;
import de.hybris.platform.servicelayer.exceptions.ModelSavingException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Model と表の行の読み書き。インターセプターや必須の確かめは DefaultModelService が行い、ここは DB だけを扱う。
 */
public class ItemPersistence
{
	private static final ObjectMapper JSON = new ObjectMapper();

	private final JdbcTemplate jdbc;
	private final TypeSystem ts;

	public ItemPersistence(final JdbcTemplate jdbc, final TypeSystem ts)
	{
		this.jdbc = jdbc;
		this.ts = ts;
	}

	public TypeSystem getTypeSystem()
	{
		return ts;
	}

	// ------------------------------------------------------------------ 読む

	/** pk の行を読む。baseType はその行が入っている表を決めるための型(親の型でよい)。 */
	public ItemModel load(final PK pk, final String baseType)
	{
		final List<ItemModel> list = loadAll(List.of(pk.getLongValue()), baseType);
		return list.isEmpty() ? null : list.get(0);
	}

	/** 型の分からない pk を読む。すべての表を順に探す。 */
	public ItemModel loadAnyType(final PK pk)
	{
		for (final String table : ts.tables())
		{
			final List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM " + table + " WHERE pk = ?", pk.getLongValue());
			if (!rows.isEmpty())
			{
				return fromRow(rows.get(0));
			}
		}
		return null;
	}

	/** pk の並びの順を保って読む。見つからない pk は飛ばす。 */
	public List<ItemModel> loadAll(final List<Long> pks, final String baseType)
	{
		if (pks.isEmpty())
		{
			return List.of();
		}
		final String table = ts.tableOf(ts.getItemType(baseType));
		final String in = pks.stream().map(p -> "?").collect(Collectors.joining(","));
		final Map<Long, ItemModel> byPk = new HashMap<>();
		jdbc.query("SELECT * FROM " + table + " WHERE pk IN (" + in + ")", (ResultSet rs) -> {
			final Map<String, Object> row = rowToMap(rs);
			byPk.put(((Number) row.get("pk")).longValue(), fromRow(row));
		}, pks.toArray());
		final List<ItemModel> out = new ArrayList<>();
		for (final Long pk : pks)
		{
			if (byPk.containsKey(pk))
			{
				out.add(byPk.get(pk));
			}
		}
		return out;
	}

	private static Map<String, Object> rowToMap(final ResultSet rs) throws SQLException
	{
		final Map<String, Object> row = new LinkedHashMap<>();
		for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++)
		{
			row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
		}
		return row;
	}

	private ItemModel fromRow(final Map<String, Object> row)
	{
		final String typeCode = (String) row.get("typecode");
		final ItemTypeDef type = ts.getItemType(typeCode);
		final ItemModel model = instantiate(typeCode);
		final ItemModel.ItemModelInternals in = model.internals();
		in.setPk(PK.fromLong(((Number) row.get("pk")).longValue()));
		in.setTimestamps(toDate(row.get("createdts")), toDate(row.get("modifiedts")));
		for (final AttributeDef a : ts.allAttributes(type).values())
		{
			if ("Item".equals(a.declaringType) || "dynamic".equals(a.persistence))
			{
				continue;
			}
			final Kind kind = ts.kindOf(a);
			if (!ts.hasColumn(a))
			{
				in.loadValue(a.qualifier, (ItemModel.LazyValue) () -> loadRelation(model, a));
				continue;
			}
			final Object raw = row.get(TypeSystem.columnName(a.qualifier));
			if (kind == Kind.LOCALIZED_STRING)
			{
				in.loadLocalized(a.qualifier, readJson(raw, new TypeReference<Map<String, String>>()
				{
				}));
			}
			else if (kind == Kind.ITEM)
			{
				if (raw != null)
				{
					final long fk = ((Number) raw).longValue();
					in.loadValue(a.qualifier, (ItemModel.LazyValue) () -> load(PK.fromLong(fk), a.type));
				}
				else
				{
					in.loadValue(a.qualifier, null);
				}
			}
			else
			{
				in.loadValue(a.qualifier, fromDb(a, kind, raw));
			}
		}
		in.clearDirty();
		return model;
	}

	public ItemModel instantiate(final String typeCode)
	{
		try
		{
			return TypeSystemHolder.modelClass(typeCode).getDeclaredConstructor().newInstance();
		}
		catch (final ReflectiveOperationException e)
		{
			throw new IllegalStateException("cannot create model for " + typeCode, e);
		}
	}

	/** 関連のコレクション(多の側)を読む。 */
	public Collection<ItemModel> loadRelation(final ItemModel owner, final AttributeDef a)
	{
		final List<Long> pks;
		if (a.relation.manyToMany())
		{
			pks = a.relationSourceSide
					? jdbc.queryForList("SELECT sourcepk FROM " + a.relation.table + " WHERE targetpk = ? ORDER BY reversesequencenumber, sourcepk",
							Long.class, owner.getPk().getLongValue())
					: jdbc.queryForList("SELECT targetpk FROM " + a.relation.table + " WHERE sourcepk = ? ORDER BY sequencenumber, targetpk",
							Long.class, owner.getPk().getLongValue());
		}
		else
		{
			final ItemTypeDef elementType = ts.getItemType(a.type);
			final String fkColumn = TypeSystem.columnName(backReferenceQualifier(a));
			final String typecodes = ts.withSubtypes(elementType).stream().map(t -> "'" + t.code + "'").collect(Collectors.joining(","));
			pks = jdbc.queryForList("SELECT pk FROM " + ts.tableOf(elementType) + " WHERE " + fkColumn + " = ? AND typecode IN (" + typecodes
					+ ") ORDER BY pk", Long.class, owner.getPk().getLongValue());
		}
		final List<ItemModel> loaded = loadAll(pks, a.type);
		final Collection<ItemModel> out = "set".equals(a.collectionKind) ? new LinkedHashSet<>(loaded) : new ArrayList<>(loaded);
		return out;
	}

	/** 一対多の関連で、多の側の型が持つ「1 の側への参照」の属性名。 */
	public String backReferenceQualifier(final AttributeDef collectionAttr)
	{
		return collectionAttr.relationSourceSide ? collectionAttr.relation.target.qualifier : collectionAttr.relation.source.qualifier;
	}

	// ------------------------------------------------------------------ 書く

	public PK nextPk()
	{
		return PK.fromLong(jdbc.queryForObject("SELECT nextval('pk_seq')", Long.class));
	}

	/**
	 * 1 件を書く。参照している未保存の Model は saveNew で先に保存してもらう(本物と同じく、新しい参照先もまとめて保存される)。
	 */
	public void write(final ItemModel model, final Consumer<ItemModel> saveNew)
	{
		final ItemModel.ItemModelInternals in = model.internals();
		final ItemTypeDef type = ts.getItemType(model.getItemtype());
		final String table = ts.tableOf(type);
		final boolean isNew = model.getPk() == null;
		final Map<String, AttributeDef> attrs = ts.allAttributes(type);
		final Date now = new Date();

		final Map<String, Object> columnValues = new LinkedHashMap<>();
		final List<AttributeDef> dirtyCollections = new ArrayList<>();
		for (final String qualifier : in.dirtyAttributes())
		{
			final AttributeDef a = attrs.get(qualifier.toLowerCase(java.util.Locale.ROOT));
			if (a == null || "Item".equals(a.declaringType) || "dynamic".equals(a.persistence))
			{
				continue;
			}
			final Kind kind = ts.kindOf(a);
			if (!ts.hasColumn(a))
			{
				dirtyCollections.add(a);
				continue;
			}
			if (kind == Kind.LOCALIZED_STRING)
			{
				columnValues.put(TypeSystem.columnName(a.qualifier), writeJson(in.localizedValues().get(a.qualifier)));
			}
			else if (kind == Kind.ITEM)
			{
				final Object v = in.peek(a.qualifier);
				if (v instanceof ItemModel ref && ref.getPk() == null)
				{
					saveNew.accept(ref);
				}
				columnValues.put(TypeSystem.columnName(a.qualifier), v instanceof ItemModel ref ? ref.getPk().getLongValue() : null);
			}
			else
			{
				columnValues.put(TypeSystem.columnName(a.qualifier), toDb(a, kind, in.peek(a.qualifier)));
			}
		}

		if (isNew)
		{
			in.setPk(nextPk());
			in.setTimestamps(now, now);
			final List<String> cols = new ArrayList<>(List.of("pk", "typecode", "createdts", "modifiedts"));
			final List<Object> vals = new ArrayList<>(List.of(model.getPk().getLongValue(), model.getItemtype(), new Timestamp(now.getTime()),
					new Timestamp(now.getTime())));
			cols.addAll(columnValues.keySet());
			vals.addAll(columnValues.values());
			final String placeholders = cols.stream().map(c -> placeholder(table, c)).collect(Collectors.joining(","));
			jdbc.update("INSERT INTO " + table + " (" + String.join(",", cols) + ") VALUES (" + placeholders + ")", vals.toArray());
		}
		else if (!columnValues.isEmpty() || !dirtyCollections.isEmpty())
		{
			model.setModifiedtime(now);
			final List<Object> vals = new ArrayList<>(columnValues.values());
			vals.add(new Timestamp(now.getTime()));
			vals.add(model.getPk().getLongValue());
			final String set = columnValues.keySet().stream().map(c -> c + " = " + placeholder(table, c)).collect(Collectors.joining(", "));
			final int rows = jdbc.update("UPDATE " + table + " SET " + (set.isEmpty() ? "" : set + ", ") + "modifiedts = ? WHERE pk = ?",
					vals.toArray());
			if (rows == 0)
			{
				throw new ModelSavingException("Entity not found ( pk = " + model.getPk() + " name = '" + model.getItemtype()
						+ "' type = '" + table + "'). The item may have been removed by another process.");
			}
		}
		for (final AttributeDef a : dirtyCollections)
		{
			writeRelation(model, a, saveNew);
		}
		in.clearDirty();
	}

	@SuppressWarnings("unchecked")
	private void writeRelation(final ItemModel owner, final AttributeDef a, final Consumer<ItemModel> saveNew)
	{
		final Object value = owner.internals().peek(a.qualifier);
		final Collection<ItemModel> elements = value instanceof Collection<?> c ? (Collection<ItemModel>) c : List.of();
		final long ownerPk = owner.getPk().getLongValue();
		if (a.relation.manyToMany())
		{
			for (final ItemModel e : elements)
			{
				if (e.getPk() == null)
				{
					saveNew.accept(e);
				}
			}
			final String ownerCol = a.relationSourceSide ? "targetpk" : "sourcepk";
			final String elemCol = a.relationSourceSide ? "sourcepk" : "targetpk";
			final String seqCol = a.relationSourceSide ? "reversesequencenumber" : "sequencenumber";
			jdbc.update("DELETE FROM " + a.relation.table + " WHERE " + ownerCol + " = ?", ownerPk);
			int seq = 0;
			for (final ItemModel e : elements)
			{
				jdbc.update("INSERT INTO " + a.relation.table + " (" + ownerCol + ", " + elemCol + ", " + seqCol + ") VALUES (?, ?, ?)"
						+ " ON CONFLICT (sourcepk, targetpk) DO UPDATE SET " + seqCol + " = EXCLUDED." + seqCol, ownerPk, e.getPk().getLongValue(),
						seq++);
			}
			return;
		}
		// 一対多: 多の側の行の「1 の側への参照」を書き換える。
		final String back = backReferenceQualifier(a);
		final List<Long> keep = new ArrayList<>();
		for (final ItemModel e : elements)
		{
			e.internals().set(back, owner);
			if (e.getPk() == null)
			{
				saveNew.accept(e);
			}
			else
			{
				write(e, saveNew);
			}
			keep.add(e.getPk().getLongValue());
		}
		final ItemTypeDef elementType = ts.getItemType(a.type);
		final String sql = "UPDATE " + ts.tableOf(elementType) + " SET " + TypeSystem.columnName(back) + " = NULL WHERE "
				+ TypeSystem.columnName(back) + " = ?" + (keep.isEmpty() ? "" : " AND pk NOT IN (" + keep.stream().map(String::valueOf)
						.collect(Collectors.joining(",")) + ")");
		jdbc.update(sql, ownerPk);
	}

	public void delete(final ItemModel model)
	{
		final ItemTypeDef type = ts.getItemType(model.getItemtype());
		final long pk = model.getPk().getLongValue();
		jdbc.update("DELETE FROM " + ts.tableOf(type) + " WHERE pk = ?", pk);
		for (final TypeSystem.RelationDef rel : ts.getRelations())
		{
			if (rel.manyToMany())
			{
				jdbc.update("DELETE FROM " + rel.table + " WHERE sourcepk = ? OR targetpk = ?", pk, pk);
			}
		}
		model.internals().markRemoved();
	}

	/** 同じ型の家族の中で、unique の属性の組が同じ行が他にあるか。 */
	public boolean existsOtherWithSameUniqueKey(final ItemModel model, final List<AttributeDef> uniqueAttrs)
	{
		final ItemTypeDef type = ts.getItemType(model.getItemtype());
		final ItemTypeDef declaring = ts.getItemType(uniqueAttrs.get(0).declaringType);
		final String typecodes = ts.withSubtypes(declaring).stream().map(t -> "'" + t.code + "'").collect(Collectors.joining(","));
		final StringBuilder sql = new StringBuilder("SELECT count(*) FROM " + ts.tableOf(type) + " WHERE typecode IN (" + typecodes + ")");
		final List<Object> params = new ArrayList<>();
		for (final AttributeDef a : uniqueAttrs)
		{
			final Object v = model.internals().get(a.qualifier);
			final Kind kind = ts.kindOf(a);
			sql.append(" AND ").append(TypeSystem.columnName(a.qualifier)).append(" = ?");
			params.add(kind == Kind.ITEM ? ((ItemModel) v).getPk().getLongValue() : toDb(a, kind, v));
		}
		if (model.getPk() != null)
		{
			sql.append(" AND pk <> ?");
			params.add(model.getPk().getLongValue());
		}
		final Integer count = jdbc.queryForObject(sql.toString(), Integer.class, params.toArray());
		return count != null && count > 0;
	}

	// ------------------------------------------------------------------ 値の変換

	private String placeholder(final String table, final String column)
	{
		final AttributeDef a = ts.columnsOfTable(table).get(column);
		if (a != null)
		{
			final Kind k = ts.kindOf(a);
			if (k == Kind.LOCALIZED_STRING || k == Kind.COLLECTION)
			{
				return "CAST(? AS jsonb)";
			}
		}
		return "?";
	}

	public Object toDb(final AttributeDef a, final Kind kind, final Object v)
	{
		if (v == null)
		{
			return null;
		}
		return switch (kind)
		{
			case DATE -> new Timestamp(((Date) v).getTime());
			case ENUM -> v instanceof HybrisEnumValue e ? e.getCode() : v.toString();
			case ITEM -> v instanceof ItemModel m ? m.getPk().getLongValue() : v;
			case COLLECTION -> writeJson(((Collection<?>) v).stream()
					.map(e -> e instanceof HybrisEnumValue ev ? ev.getCode() : e instanceof ItemModel m ? m.getPk().getLongValue() : e).toList());
			case DECIMAL -> v instanceof BigDecimal ? v : new BigDecimal(v.toString());
			default -> v;
		};
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object fromDb(final AttributeDef a, final Kind kind, final Object raw)
	{
		if (raw == null)
		{
			return null;
		}
		switch (kind)
		{
			case DATE:
				return toDate(raw);
			case ENUM:
				return enumValue(a.type, raw.toString());
			case COLLECTION:
			{
				final List<Object> list = readJson(raw, new TypeReference<List<Object>>()
				{
				});
				final String elementType = ts.findCollectionType(a.type).map(TypeSystem.CollectionTypeDef::elementType).orElse("java.lang.String");
				if (ts.findEnumType(elementType).isPresent())
				{
					return new ArrayList<>(list.stream().map(o -> enumValue(elementType, o.toString())).toList());
				}
				return list == null ? new ArrayList<>() : new ArrayList<>(list);
			}
			case INTEGER:
				return ((Number) raw).intValue();
			case LONG:
				return ((Number) raw).longValue();
			case DOUBLE:
				return ((Number) raw).doubleValue();
			default:
				return raw;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object enumValue(final String enumType, final String code)
	{
		final Class<? extends Enum<?>> c = TypeSystemHolder.enumClass(enumType);
		if (c == null)
		{
			throw new IllegalArgumentException("enum '" + enumType + "' がありません");
		}
		for (final Enum<?> e : c.getEnumConstants())
		{
			if (((HybrisEnumValue) e).getCode().equalsIgnoreCase(code))
			{
				return e;
			}
		}
		throw new IllegalArgumentException("enum '" + enumType + "' に値 '" + code + "' がありません");
	}

	private static Date toDate(final Object raw)
	{
		if (raw == null)
		{
			return null;
		}
		if (raw instanceof Timestamp t)
		{
			return new Date(t.getTime());
		}
		if (raw instanceof java.time.OffsetDateTime o)
		{
			return Date.from(o.toInstant());
		}
		if (raw instanceof Date d)
		{
			return d;
		}
		throw new IllegalArgumentException("not a date: " + raw.getClass());
	}

	private static String writeJson(final Object value)
	{
		if (value == null)
		{
			return null;
		}
		try
		{
			return JSON.writeValueAsString(value);
		}
		catch (final Exception e)
		{
			throw new IllegalStateException(e);
		}
	}

	private static <T> T readJson(final Object raw, final TypeReference<T> type)
	{
		if (raw == null)
		{
			return null;
		}
		try
		{
			return JSON.readValue(raw.toString(), type);
		}
		catch (final Exception e)
		{
			throw new IllegalStateException("bad json in db: " + raw, e);
		}
	}
}
