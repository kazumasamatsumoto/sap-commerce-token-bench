package de.hybris.platform.category.impl;

import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.category.CategoryService;
import de.hybris.platform.category.model.CategoryModel;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import java.util.List;
import java.util.Map;

public class DefaultCategoryService extends AbstractItemDao implements CategoryService
{
	private CatalogVersionService catalogVersionService;

	@Override
	public CategoryModel getCategoryForCode(final String code)
	{
		final List<CategoryModel> list = find("SELECT {pk} FROM {Category} WHERE {code} = ?code AND {catalogVersion} IN (?versions)",
				Map.of("code", code, "versions", catalogVersionService.getSessionCatalogVersions()));
		if (list.isEmpty())
		{
			throw new UnknownIdentifierException("Category with code '" + code + "' not found! (Active session catalogversions: "
					+ catalogVersionService.getSessionCatalogVersions() + ")");
		}
		return list.get(0);
	}

	@Override
	public List<CategoryModel> getRootCategoriesForCatalogVersion(final CatalogVersionModel catalogVersion)
	{
		return find("SELECT {c.pk} FROM {Category AS c} WHERE {c.catalogVersion} = ?cv AND NOT EXISTS ({{ SELECT {r.pk} FROM {CategoryCategoryRelation AS r}"
				+ " WHERE {r.target} = {c.pk} }}) ORDER BY {c.code}", Map.of("cv", catalogVersion));
	}

	public void setCatalogVersionService(final CatalogVersionService catalogVersionService)
	{
		this.catalogVersionService = catalogVersionService;
	}
}
