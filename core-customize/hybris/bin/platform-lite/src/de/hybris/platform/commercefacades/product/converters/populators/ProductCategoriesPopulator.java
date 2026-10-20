package de.hybris.platform.commercefacades.product.converters.populators;

import de.hybris.platform.category.model.CategoryModel;
import de.hybris.platform.commercefacades.product.data.CategoryData;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;

public class ProductCategoriesPopulator implements Populator<ProductModel, ProductData>
{
	private Converter<CategoryModel, CategoryData> categoryConverter;

	@Override
	public void populate(final ProductModel source, final ProductData target)
	{
		target.setCategories(categoryConverter.convertAll(source.getSupercategories()));
	}

	public void setCategoryConverter(final Converter<CategoryModel, CategoryData> categoryConverter)
	{
		this.categoryConverter = categoryConverter;
	}
}
