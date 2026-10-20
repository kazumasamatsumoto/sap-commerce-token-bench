package de.hybris.platform.commercefacades.search;

import de.hybris.platform.commerceservices.search.facetdata.ProductSearchPageData;
import java.util.List;

public interface ProductSearchFacade
{
	ProductSearchPageData textSearch(String query, int currentPage, int pageSize, String sort);

	List<String> getAutocompleteSuggestions(String term, int max);
}
