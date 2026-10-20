package de.hybris.platform.servicelayer.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** FlexibleSearch の問い合わせ。本物と同じく、? の付いた名前で値を渡す。 */
public class FlexibleSearchQuery
{
	private final String query;
	private final Map<String, Object> queryParameters = new HashMap<>();
	private List<Class<?>> resultClassList = new ArrayList<>();
	private int start = 0;
	private int count = -1;
	private boolean needTotal = false;

	public FlexibleSearchQuery(final String query)
	{
		this.query = query;
	}

	public FlexibleSearchQuery(final String query, final Map<String, ?> queryParameters)
	{
		this.query = query;
		this.queryParameters.putAll(queryParameters);
	}

	public String getQuery()
	{
		return query;
	}

	public void addQueryParameter(final String key, final Object value)
	{
		queryParameters.put(key, value);
	}

	public void addQueryParameters(final Map<String, ?> params)
	{
		queryParameters.putAll(params);
	}

	public Map<String, Object> getQueryParameters()
	{
		return queryParameters;
	}

	public List<Class<?>> getResultClassList()
	{
		return resultClassList;
	}

	public void setResultClassList(final List<Class<?>> resultClassList)
	{
		this.resultClassList = resultClassList;
	}

	public int getStart()
	{
		return start;
	}

	public void setStart(final int start)
	{
		this.start = start;
	}

	public int getCount()
	{
		return count;
	}

	public void setCount(final int count)
	{
		this.count = count;
	}

	public boolean isNeedTotal()
	{
		return needTotal;
	}

	public void setNeedTotal(final boolean needTotal)
	{
		this.needTotal = needTotal;
	}

	@Override
	public String toString()
	{
		return "FlexibleSearchQuery[" + query + ", params=" + queryParameters + "]";
	}
}
