package de.hybris.platform.servicelayer.search.impl;

import de.hybris.bootstrap.typesystem.TypeSystem;
import de.hybris.bootstrap.typesystem.TypeSystem.AttributeDef;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.lite.persistence.ItemPersistence;
import de.hybris.platform.lite.search.FlexibleSearchTranslator;
import de.hybris.platform.servicelayer.exceptions.AmbiguousIdentifierException;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.i18n.I18NService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.servicelayer.search.exceptions.FlexibleSearchException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class DefaultFlexibleSearchService implements FlexibleSearchService
{
	private static final Logger LOG = LoggerFactory.getLogger(DefaultFlexibleSearchService.class);

	private final JdbcTemplate jdbc;
	private final ItemPersistence persistence;
	private final FlexibleSearchTranslator translator;
	private final I18NService i18nService;

	public DefaultFlexibleSearchService(final JdbcTemplate jdbc, final ItemPersistence persistence, final I18NService i18nService)
	{
		this.jdbc = jdbc;
		this.persistence = persistence;
		this.translator = new FlexibleSearchTranslator(persistence.getTypeSystem());
		this.i18nService = i18nService;
	}

	@Override
	public <T> SearchResult<T> search(final String query)
	{
		return search(new FlexibleSearchQuery(query));
	}

	@Override
	public <T> SearchResult<T> search(final String query, final Map<String, ?> queryParams)
	{
		return search(new FlexibleSearchQuery(query, queryParams));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> SearchResult<T> search(final FlexibleSearchQuery query)
	{
		final FlexibleSearchTranslator.Translated t = translator.translate(query.getQuery(), query.getQueryParameters(),
				i18nService.getCurrentLocale().getLanguage());
		String sql = t.sql();
		if (query.getCount() >= 0)
		{
			sql += " LIMIT " + query.getCount();
		}
		if (query.getStart() > 0)
		{
			sql += " OFFSET " + query.getStart();
		}
		LOG.debug("FlexibleSearch: {} -> {} {}", query.getQuery(), sql, t.params());
		try
		{
			final List<Object> result;
			final boolean asModels = t.selectsOnlyPk() && (query.getResultClassList().isEmpty()
					|| ItemModel.class.isAssignableFrom(query.getResultClassList().get(0)));
			if (asModels)
			{
				final List<Long> pks = jdbc.queryForList(sql, Long.class, t.params().toArray());
				result = new ArrayList<>(persistence.loadAll(pks, t.selectType()));
			}
			else if (query.getResultClassList().size() > 1)
			{
				result = new ArrayList<>(jdbc.query(sql, (rs, i) -> {
					final List<Object> row = new ArrayList<>();
					for (int c = 1; c <= query.getResultClassList().size(); c++)
					{
						row.add(convert(rs.getObject(c), query.getResultClassList().get(c - 1)));
					}
					return row;
				}, t.params().toArray()));
			}
			else
			{
				final Class<?> type = query.getResultClassList().isEmpty() ? Object.class : query.getResultClassList().get(0);
				result = new ArrayList<>(jdbc.query(sql, (rs, i) -> convert(rs.getObject(1), type), t.params().toArray()));
			}
			int total = result.size();
			if (query.isNeedTotal())
			{
				final Integer count = jdbc.queryForObject("SELECT count(*) FROM (" + t.sql() + ") total_q", Integer.class, t.params().toArray());
				total = count == null ? 0 : count;
			}
			return (SearchResult<T>) new DefaultSearchResult<>(result, total, query.getStart(), query.getCount());
		}
		catch (final DataAccessException e)
		{
			throw new FlexibleSearchException("SQL search error - " + rootMessage(e) + " query = '" + sql + "', values = " + t.params(), e);
		}
	}

	private static Object convert(final Object raw, final Class<?> type)
	{
		if (raw == null)
		{
			return null;
		}
		if (type == Integer.class && raw instanceof Number n)
		{
			return n.intValue();
		}
		if (type == Long.class && raw instanceof Number n)
		{
			return n.longValue();
		}
		if (type == Double.class && raw instanceof Number n)
		{
			return n.doubleValue();
		}
		if (type == String.class)
		{
			return raw.toString();
		}
		return raw;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T searchUnique(final FlexibleSearchQuery query)
	{
		final List<T> result = (List<T>) search(query).getResult();
		if (result.isEmpty())
		{
			throw new ModelNotFoundException("No result for the given query");
		}
		if (result.size() > 1)
		{
			throw new AmbiguousIdentifierException("Found " + result.size() + " results for the given query");
		}
		return result.get(0);
	}

	@Override
	public <T> T getModelByExample(final T example)
	{
		final List<T> list = getModelsByExample(example);
		if (list.isEmpty())
		{
			throw new ModelNotFoundException("No result for the given example [" + example + "] was found.");
		}
		if (list.size() > 1)
		{
			throw new AmbiguousIdentifierException("More than one result for the given example [" + example + "] was found.");
		}
		return list.get(0);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> List<T> getModelsByExample(final T example)
	{
		final ItemModel m = (ItemModel) example;
		final TypeSystem ts = persistence.getTypeSystem();
		final StringBuilder q = new StringBuilder("SELECT {pk} FROM {" + m.getItemtype() + "}");
		final Map<String, Object> params = new HashMap<>();
		String sep = " WHERE ";
		for (final String qualifier : m.internals().dirtyAttributes())
		{
			final AttributeDef a = ts.findAttribute(ts.getItemType(m.getItemtype()), qualifier).orElse(null);
			if (a == null || !ts.hasColumn(a) || a.isLocalized() || "Item".equals(a.declaringType))
			{
				continue;
			}
			q.append(sep).append('{').append(a.qualifier).append("} = ?").append(a.qualifier);
			params.put(a.qualifier, m.internals().get(a.qualifier));
			sep = " AND ";
		}
		return (List<T>) search(q.toString(), params).getResult();
	}

	private static String rootMessage(final Throwable t)
	{
		Throwable c = t;
		while (c.getCause() != null)
		{
			c = c.getCause();
		}
		return c.getMessage();
	}

	public record DefaultSearchResult<E>(List<E> result, int totalCount, int requestedStart, int requestedCount) implements SearchResult<E>
	{
		@Override
		public List<E> getResult()
		{
			return result;
		}

		@Override
		public int getTotalCount()
		{
			return totalCount;
		}

		@Override
		public int getCount()
		{
			return result.size();
		}

		@Override
		public int getRequestedStart()
		{
			return requestedStart;
		}

		@Override
		public int getRequestedCount()
		{
			return requestedCount;
		}
	}
}
