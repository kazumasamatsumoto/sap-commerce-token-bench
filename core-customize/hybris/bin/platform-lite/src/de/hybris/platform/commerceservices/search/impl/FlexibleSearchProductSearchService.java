package de.hybris.platform.commerceservices.search.impl;

import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.category.model.CategoryModel;
import de.hybris.platform.commerceservices.search.ProductSearchService;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.i18n.I18NService;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.SearchResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FlexibleSearchProductSearchService extends AbstractItemDao implements ProductSearchService
{
	private CatalogVersionService catalogVersionService;
	private CommonI18NService commonI18NService;
	private I18NService i18NService;

	@Override
	public Result search(final String query, final int currentPage, final int pageSize, final String sortParam)
	{
		final String[] parts = (query == null ? "" : query).split(":", -1);
		final String freeText = parts.length > 0 ? parts[0].trim() : "";
		String sort = parts.length > 1 && !parts[1].isBlank() ? parts[1] : "relevance";
		if (sortParam != null && !sortParam.isBlank())
		{
			sort = sortParam;
		}
		final Map<String, List<String>> filters = new LinkedHashMap<>();
		for (int i = 2; i + 1 < parts.length; i += 2)
		{
			filters.computeIfAbsent(parts[i], k -> new ArrayList<>()).add(parts[i + 1]);
		}

		final Map<String, Object> params = new HashMap<>();
		final String where = where(freeText, filters, params);
		final String lang = i18NService.getCurrentLocale().getLanguage();
		final String orderBy = switch (sort)
		{
			case "name-asc" -> " ORDER BY {p.name[" + lang + "]} ASC, {p.code}";
			case "name-desc" -> " ORDER BY {p.name[" + lang + "]} DESC, {p.code}";
			case "topRated" -> " ORDER BY {p.averageRating} DESC, {p.code}";
			case "price-asc" -> " ORDER BY ({{ SELECT min({pr.price}) FROM {PriceRow AS pr} WHERE {pr.product} = {p.pk} AND {pr.currency} = ?currency }}) ASC, {p.code}";
			case "price-desc" -> " ORDER BY ({{ SELECT min({pr.price}) FROM {PriceRow AS pr} WHERE {pr.product} = {p.pk} AND {pr.currency} = ?currency }}) DESC, {p.code}";
			default -> " ORDER BY {p.code}";
		};
		if (sort.startsWith("price"))
		{
			params.put("currency", commonI18NService.getCurrentCurrency());
		}
		final FlexibleSearchQuery q = new FlexibleSearchQuery("SELECT {p.pk} FROM {Product AS p}" + where + orderBy, params);
		q.setStart(currentPage * pageSize);
		q.setCount(pageSize);
		q.setNeedTotal(true);
		final SearchResult<ProductModel> result = getFlexibleSearchService().search(q);
		return new Result(freeText, sort, filters, result.getResult(), result.getTotalCount(), currentPage, pageSize,
				List.of(categoryFacet(freeText, filters), brandFacet(freeText, filters)));
	}

	private String where(final String freeText, final Map<String, List<String>> filters, final Map<String, Object> params)
	{
		final StringBuilder w = new StringBuilder(" WHERE {p.catalogVersion} IN (?versions) AND {p.approvalStatus} = 'approved'");
		params.put("versions", catalogVersionService.getSessionCatalogVersions());
		if (!freeText.isEmpty())
		{
			final String lang = i18NService.getCurrentLocale().getLanguage();
			w.append(" AND ({p.code} ILIKE ?text OR {p.name[").append(lang).append("]} ILIKE ?text OR {p.summary[").append(lang)
					.append("]} ILIKE ?text OR {p.manufacturerName} ILIKE ?text)");
			params.put("text", "%" + freeText + "%");
		}
		int i = 0;
		for (final String cat : filters.getOrDefault("category", List.of()))
		{
			final String p = "cat" + i++;
			w.append(" AND EXISTS ({{ SELECT {r.pk} FROM {CategoryProductRelation AS r JOIN Category AS c ON {r.source} = {c.pk}} WHERE {r.target} = {p.pk}")
					.append(" AND ({c.code} = ?").append(p).append(" OR EXISTS ({{ SELECT {cc.pk} FROM {CategoryCategoryRelation AS cc JOIN Category AS sc ON {cc.source} = {sc.pk}}")
					.append(" WHERE {cc.target} = {c.pk} AND {sc.code} = ?").append(p).append(" }})) }})");
			params.put(p, cat);
		}
		int b = 0;
		for (final String brand : filters.getOrDefault("brand", List.of()))
		{
			final String p = "brand" + b++;
			w.append(" AND EXISTS ({{ SELECT {b.pk} FROM {Brand AS b} WHERE {b.pk} = {p.brand} AND {b.code} = ?").append(p).append(" }})");
			params.put(p, brand);
		}
		return w.toString();
	}

	/** カテゴリごとの件数(上位 10 件)。今の絞り込みのうちカテゴリ以外は効かせる。 */
	private Facet categoryFacet(final String freeText, final Map<String, List<String>> filters)
	{
		final Map<String, List<String>> others = new LinkedHashMap<>(filters);
		others.remove("category");
		final Map<String, Object> params = new HashMap<>();
		final String where = where(freeText, others, params);
		final FlexibleSearchQuery q = new FlexibleSearchQuery("SELECT {c.code}, count({p.pk}) FROM {Product AS p JOIN CategoryProductRelation AS r ON {r.target} = {p.pk}"
				+ " JOIN Category AS c ON {r.source} = {c.pk}}" + where + " GROUP BY {c.code} ORDER BY count({p.pk}) DESC, {c.code} LIMIT 10", params);
		q.setResultClassList(List.of(String.class, Long.class));
		final List<FacetValue> values = new ArrayList<>();
		for (final Object row : getFlexibleSearchService().search(q).getResult())
		{
			final List<?> r = (List<?>) row;
			final String code = (String) r.get(0);
			values.add(new FacetValue(code, categoryName(code), (Long) r.get(1), filters.getOrDefault("category", List.of()).contains(code)));
		}
		return new Facet("category", "カテゴリ", true, values);
	}

	private Facet brandFacet(final String freeText, final Map<String, List<String>> filters)
	{
		final Map<String, List<String>> others = new LinkedHashMap<>(filters);
		others.remove("brand");
		final Map<String, Object> params = new HashMap<>();
		final String where = where(freeText, others, params);
		final String lang = i18NService.getCurrentLocale().getLanguage();
		final FlexibleSearchQuery q = new FlexibleSearchQuery("SELECT {b.code}, {b.name[" + lang + "]}, count({p.pk}) FROM {Product AS p JOIN Brand AS b ON {p.brand} = {b.pk}}"
				+ where + " GROUP BY {b.code}, {b.name[" + lang + "]} ORDER BY count({p.pk}) DESC, {b.code} LIMIT 10", params);
		q.setResultClassList(List.of(String.class, String.class, Long.class));
		final List<FacetValue> values = new ArrayList<>();
		for (final Object row : getFlexibleSearchService().search(q).getResult())
		{
			final List<?> r = (List<?>) row;
			values.add(new FacetValue((String) r.get(0), (String) r.get(1), (Long) r.get(2), filters.getOrDefault("brand", List.of()).contains(r.get(0))));
		}
		return new Facet("brand", "ブランド", false, values);
	}

	private String categoryName(final String code)
	{
		final List<CategoryModel> list = find("SELECT {pk} FROM {Category} WHERE {code} = ?code AND {catalogVersion} IN (?versions)",
				Map.of("code", code, "versions", catalogVersionService.getSessionCatalogVersions()));
		return list.isEmpty() ? code : list.get(0).getName();
	}

	@Override
	public List<String> getSuggestions(final String term, final int max)
	{
		final String lang = i18NService.getCurrentLocale().getLanguage();
		final FlexibleSearchQuery q = new FlexibleSearchQuery("SELECT DISTINCT {name[" + lang + "]} FROM {Product} WHERE {name[" + lang
				+ "]} ILIKE ?term AND {catalogVersion} IN (?versions) ORDER BY {name[" + lang + "]}",
				Map.of("term", term + "%", "versions", catalogVersionService.getSessionCatalogVersions()));
		q.setResultClassList(List.of(String.class));
		q.setCount(max);
		return getFlexibleSearchService().<String> search(q).getResult();
	}

	public void setCatalogVersionService(final CatalogVersionService catalogVersionService)
	{
		this.catalogVersionService = catalogVersionService;
	}

	public void setCommonI18NService(final CommonI18NService commonI18NService)
	{
		this.commonI18NService = commonI18NService;
	}

	public void setI18NService(final I18NService i18NService)
	{
		this.i18NService = i18NService;
	}
}
