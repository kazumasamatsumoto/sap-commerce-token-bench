package com.xyz.facades.brand.populators;

import com.xyz.core.model.BrandModel;
import com.xyz.facades.brand.data.BrandData;
import de.hybris.platform.commercefacades.product.data.ImageData;
import de.hybris.platform.converters.Populator;

public class BrandPopulator implements Populator<BrandModel, BrandData>
{
	@Override
	public void populate(final BrandModel source, final BrandData target)
	{
		target.setCode(source.getCode());
		target.setName(source.getName());
		target.setDescription(source.getDescription());
		target.setTier(source.getTier() == null ? null : source.getTier().getCode());
		if (source.getLogo() != null)
		{
			final ImageData logo = new ImageData();
			logo.setUrl(source.getLogo().getURL());
			logo.setAltText(source.getName());
			logo.setFormat("logo");
			target.setLogo(logo);
		}
	}
}
