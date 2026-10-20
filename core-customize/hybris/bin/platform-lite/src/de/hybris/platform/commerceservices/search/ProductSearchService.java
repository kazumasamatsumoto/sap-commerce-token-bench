package de.hybris.platform.commerceservices.search;

import de.hybris.platform.core.model.product.ProductModel;
import java.util.List;
import java.util.Map;

/**
 * 商品の検索。本物は Solr を使うが、疑似プラットフォームは FlexibleSearch で同じ形の結果を返す。
 * query は OCC と同じ書き方: "<自由語>:<並べ替え>:<絞り込みの名前>:<値>:..."(例 "テレビ:price-asc:category:cat-01-03")。
 */
public interface ProductSearchService
{
	record FacetValue(String code, String name, long count, boolean selected)
	{
	}

	record Facet(String code, String name, boolean category, List<FacetValue> values)
	{
	}

	record Result(String freeText, String sort, Map<String, List<String>> filters, List<ProductModel> products, long totalCount, int currentPage,
			int pageSize, List<Facet> facets)
	{
	}

	List<String> SORTS = List.of("relevance", "topRated", "name-asc", "name-desc", "price-asc", "price-desc");

	Result search(String query, int currentPage, int pageSize, String sort);

	List<String> getSuggestions(String term, int max);
}
