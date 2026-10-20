package de.hybris.platform.category;

import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.category.model.CategoryModel;
import java.util.List;

public interface CategoryService
{
	CategoryModel getCategoryForCode(String code);

	List<CategoryModel> getRootCategoriesForCatalogVersion(CatalogVersionModel catalogVersion);
}
