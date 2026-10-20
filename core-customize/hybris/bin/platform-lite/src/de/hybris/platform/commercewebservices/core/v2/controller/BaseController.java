package de.hybris.platform.commercewebservices.core.v2.controller;

import de.hybris.platform.commercewebservicescommons.dto.search.pagedata.PaginationWsDTO;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper;
import jakarta.annotation.Resource;

/** OCC の Controller の土台。DataMapper と、よく使う既定値を持つ。 */
public abstract class BaseController
{
	protected static final String DEFAULT_FIELD_SET = FieldSetLevelHelper.DEFAULT_LEVEL;
	protected static final String BASIC_FIELD_SET = FieldSetLevelHelper.BASIC_LEVEL;
	protected static final String DEFAULT_CURRENT_PAGE = "0";
	protected static final String DEFAULT_PAGE_SIZE = "20";
	protected static final int MAX_PAGE_SIZE = 100;

	@Resource(name = "dataMapper")
	private DataMapper dataMapper;

	protected DataMapper getDataMapper()
	{
		return dataMapper;
	}

	protected static int pageSize(final int requested)
	{
		return Math.max(1, Math.min(requested, MAX_PAGE_SIZE));
	}

	protected static PaginationWsDTO pagination(final SearchResult<?> result, final int currentPage, final int pageSize, final String sort)
	{
		final PaginationWsDTO p = new PaginationWsDTO();
		p.setCurrentPage(currentPage);
		p.setPageSize(pageSize);
		p.setTotalResults((long) result.getTotalCount());
		p.setTotalPages((int) Math.ceil(result.getTotalCount() / (double) pageSize));
		p.setSort(sort);
		return p;
	}
}
