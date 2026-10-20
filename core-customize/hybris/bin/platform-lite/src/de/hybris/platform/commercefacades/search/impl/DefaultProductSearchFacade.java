package de.hybris.platform.commercefacades.search.impl;

import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.search.ProductSearchFacade;
import de.hybris.platform.commercefacades.search.data.SearchQueryData;
import de.hybris.platform.commercefacades.search.data.SearchStateData;
import de.hybris.platform.commerceservices.search.ProductSearchService;
import de.hybris.platform.commerceservices.search.facetdata.FacetData;
import de.hybris.platform.commerceservices.search.facetdata.FacetValueData;
import de.hybris.platform.commerceservices.search.facetdata.ProductSearchPageData;
import de.hybris.platform.commerceservices.search.pagedata.PaginationData;
import de.hybris.platform.commerceservices.search.pagedata.SortData;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 一覧と検索の結果。商品は productSearchResultConverter(一覧用の軽い Converter)で作る。 */
public class DefaultProductSearchFacade implements ProductSearchFacade
{
	private static final Map<String, String> SORT_NAMES = Map.of("relevance", "関連度", "topRated", "評価の高い順", "name-asc", "名前(昇順)", "name-desc",
			"名前(降順)", "price-asc", "価格の安い順", "price-desc", "価格の高い順");

	private ProductSearchService productSearchService;
	private Converter<ProductModel, ProductData> productSearchResultConverter;

	@Override
	public ProductSearchPageData textSearch(final String query, final int currentPage, final int pageSize, final String sort)
	{
		final ProductSearchService.Result r = productSearchService.search(query, currentPage, pageSize, sort);
		final ProductSearchPageData page = new ProductSearchPageData();
		page.setFreeTextSearch(r.freeText());
		page.setCurrentQuery(state(queryString(r.freeText(), r.sort(), r.filters())));
		page.setResults(productSearchResultConverter.convertAll(r.products()));
		final PaginationData p = new PaginationData();
		p.setCurrentPage(currentPage);
		p.setPageSize(pageSize);
		p.setTotalNumberOfResults(r.totalCount());
		p.setTotalPages((int) Math.ceil(r.totalCount() / (double) Math.max(1, pageSize)));
		p.setSort(r.sort());
		page.setPagination(p);
		final List<SortData> sorts = new ArrayList<>();
		for (final String code : ProductSearchService.SORTS)
		{
			final SortData s = new SortData();
			s.setCode(code);
			s.setName(SORT_NAMES.get(code));
			s.setSelected(code.equals(r.sort()));
			sorts.add(s);
		}
		page.setSorts(sorts);
		final List<FacetData> facets = new ArrayList<>();
		for (final ProductSearchService.Facet f : r.facets())
		{
			final FacetData fd = new FacetData();
			fd.setCode(f.code());
			fd.setName(f.name());
			fd.setCategory(f.category());
			fd.setMultiSelect(true);
			fd.setVisible(true);
			final List<FacetValueData> values = new ArrayList<>();
			for (final ProductSearchService.FacetValue v : f.values())
			{
				final FacetValueData vd = new FacetValueData();
				vd.setCode(v.code());
				vd.setName(v.name());
				vd.setCount(v.count());
				vd.setSelected(v.selected());
				vd.setQuery(state(queryString(r.freeText(), r.sort(), toggled(r.filters(), f.code(), v.code()))));
				values.add(vd);
			}
			fd.setValues(values);
			facets.add(fd);
		}
		page.setFacets(facets);
		return page;
	}

	private static Map<String, List<String>> toggled(final Map<String, List<String>> filters, final String key, final String value)
	{
		final Map<String, List<String>> copy = new java.util.LinkedHashMap<>();
		filters.forEach((k, v) -> copy.put(k, new ArrayList<>(v)));
		final List<String> values = copy.computeIfAbsent(key, k -> new ArrayList<>());
		if (!values.remove(value))
		{
			values.add(value);
		}
		return copy;
	}

	private static String queryString(final String freeText, final String sort, final Map<String, List<String>> filters)
	{
		final StringBuilder b = new StringBuilder(freeText == null ? "" : freeText).append(':').append(sort);
		filters.forEach((k, values) -> values.forEach(v -> b.append(':').append(k).append(':').append(v)));
		return b.toString();
	}

	private static SearchStateData state(final String query)
	{
		final SearchStateData s = new SearchStateData();
		final SearchQueryData q = new SearchQueryData();
		q.setValue(query);
		s.setQuery(q);
		s.setUrl("/search?q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8));
		return s;
	}

	@Override
	public List<String> getAutocompleteSuggestions(final String term, final int max)
	{
		return productSearchService.getSuggestions(term, max);
	}

	public void setProductSearchService(final ProductSearchService productSearchService)
	{
		this.productSearchService = productSearchService;
	}

	public void setProductSearchResultConverter(final Converter<ProductModel, ProductData> productSearchResultConverter)
	{
		this.productSearchResultConverter = productSearchResultConverter;
	}
}
