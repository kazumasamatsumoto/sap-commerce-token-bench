package de.hybris.platform.servicelayer.search;

import java.util.Map;

public interface FlexibleSearchService
{
	<T> SearchResult<T> search(FlexibleSearchQuery query);

	<T> SearchResult<T> search(String query);

	<T> SearchResult<T> search(String query, Map<String, ?> queryParams);

	/** 結果がちょうど 1 件のとき返す。0 件は ModelNotFoundException、2 件以上は AmbiguousIdentifierException。 */
	<T> T searchUnique(FlexibleSearchQuery query);

	/** 例の Model と同じ値の属性で探す(値の入っている属性だけを条件にする)。 */
	<T> T getModelByExample(T example);

	<T> java.util.List<T> getModelsByExample(T example);
}
