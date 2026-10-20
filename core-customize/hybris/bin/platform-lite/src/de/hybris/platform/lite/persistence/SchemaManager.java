package de.hybris.platform.lite.persistence;

import de.hybris.bootstrap.typesystem.TypeSystem;
import de.hybris.bootstrap.typesystem.TypeSystem.AttributeDef;
import de.hybris.bootstrap.typesystem.TypeSystem.RelationDef;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 型の情報から DB の表を作る。ant initialize は全部作り直し、ant updatesystem は足りない表と列だけ足す(本物と同じく、列は消さない)。
 */
public class SchemaManager
{
	private static final Logger LOG = LoggerFactory.getLogger(SchemaManager.class);

	private final JdbcTemplate jdbc;
	private final TypeSystem ts;

	public SchemaManager(final JdbcTemplate jdbc, final TypeSystem ts)
	{
		this.jdbc = jdbc;
		this.ts = ts;
	}

	public void dropAll()
	{
		LOG.info("dropping all tables ...");
		jdbc.execute("DROP SCHEMA IF EXISTS public CASCADE");
		jdbc.execute("CREATE SCHEMA public");
	}

	/** 足りない表と列を足す。足したものの数を返す。 */
	public int createOrUpdate()
	{
		int changes = 0;
		jdbc.execute("CREATE SEQUENCE IF NOT EXISTS pk_seq START WITH 8796093022209 INCREMENT BY 1");
		for (final String table : ts.tables())
		{
			changes += ensureTable(table, ts.columnsOfTable(table));
		}
		for (final RelationDef rel : ts.getRelations())
		{
			if (rel.manyToMany())
			{
				changes += ensureRelationTable(rel);
			}
		}
		return changes;
	}

	private int ensureTable(final String table, final Map<String, AttributeDef> columns)
	{
		int changes = 0;
		final Set<String> existing = existingColumns(table);
		if (existing.isEmpty())
		{
			LOG.info("creating table {} ({} columns)", table, columns.size() + 4);
			jdbc.execute("CREATE TABLE " + table + " (pk BIGINT PRIMARY KEY, typecode VARCHAR(255) NOT NULL, createdts TIMESTAMPTZ, modifiedts TIMESTAMPTZ)");
			jdbc.execute("CREATE INDEX IF NOT EXISTS " + table + "_typecode ON " + table + " (typecode)");
			changes++;
		}
		for (final Map.Entry<String, AttributeDef> c : columns.entrySet())
		{
			if (!existing.contains(c.getKey()))
			{
				if (!existing.isEmpty())
				{
					LOG.info("adding column {}.{} ({}.{} from extension {})", table, c.getKey(), c.getValue().declaringType,
							c.getValue().qualifier, c.getValue().extensionName);
				}
				jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + c.getKey() + " " + sqlType(c.getValue()));
				if (ts.kindOf(c.getValue()) == TypeSystem.Kind.ITEM)
				{
					jdbc.execute("CREATE INDEX IF NOT EXISTS " + table + "_" + c.getKey() + " ON " + table + " (" + c.getKey() + ")");
				}
				changes++;
			}
		}
		return changes;
	}

	private int ensureRelationTable(final RelationDef rel)
	{
		if (!existingColumns(rel.table).isEmpty())
		{
			return 0;
		}
		LOG.info("creating relation table {} for {}", rel.table, rel.code);
		jdbc.execute("CREATE TABLE " + rel.table
				+ " (sourcepk BIGINT NOT NULL, targetpk BIGINT NOT NULL, sequencenumber INT, reversesequencenumber INT, PRIMARY KEY (sourcepk, targetpk))");
		jdbc.execute("CREATE INDEX IF NOT EXISTS " + rel.table + "_target ON " + rel.table + " (targetpk)");
		return 1;
	}

	private Set<String> existingColumns(final String table)
	{
		final List<String> cols = jdbc.queryForList(
				"SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ?", String.class,
				table.toLowerCase(Locale.ROOT));
		return new HashSet<>(cols);
	}

	private String sqlType(final AttributeDef a)
	{
		return switch (ts.kindOf(a))
		{
			case STRING -> "TEXT";
			case INTEGER -> "INTEGER";
			case LONG -> "BIGINT";
			case DOUBLE -> "DOUBLE PRECISION";
			case BOOLEAN -> "BOOLEAN";
			case DATE -> "TIMESTAMPTZ";
			case DECIMAL -> "NUMERIC(30, 8)";
			case LOCALIZED_STRING, COLLECTION -> "JSONB";
			case ENUM -> "VARCHAR(255)";
			case ITEM -> "BIGINT";
			case UNKNOWN -> throw new IllegalStateException("unknown type " + a.type);
		};
	}
}
