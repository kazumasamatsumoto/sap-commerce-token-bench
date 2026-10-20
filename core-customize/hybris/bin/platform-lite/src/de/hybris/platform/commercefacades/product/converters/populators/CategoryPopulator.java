package de.hybris.platform.commercefacades.product.converters.populators;

import de.hybris.platform.category.model.CategoryModel;
import de.hybris.platform.commercefacades.product.data.CategoryData;
import de.hybris.platform.commercefacades.product.data.ImageData;
import de.hybris.platform.converters.Populator;

public class CategoryPopulator implements Populator<CategoryModel, CategoryData>
{
	@Override
	public void populate(final CategoryModel source, final CategoryData target)
	{
		target.setCode(source.getCode());
		target.setName(source.getName());
		target.setUrl("/category/" + source.getCode());
		if (source.getPicture() != null)
		{
			final ImageData image = new ImageData();
			image.setUrl(source.getPicture().getURL());
			image.setAltText(source.getName());
			image.setFormat("thumbnail");
			target.setImage(image);
		}
	}
}
