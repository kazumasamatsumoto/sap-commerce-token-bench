package com.xyz.facades.product.populators;

import com.xyz.core.model.BrandModel;
import com.xyz.facades.brand.data.BrandData;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;

/** 商品の Data に、ブランド・新着の印・素材を足す。productConverter と productSearchResultConverter の両方に入れる。 */
public class XyzProductBrandPopulator implements Populator<ProductModel, ProductData>
{
	private Converter<BrandModel, BrandData> brandConverter;

	@Override
	public void populate(final ProductModel source, final ProductData target)
	{
		if (source.getBrand() != null && !Boolean.FALSE.equals(source.getBrand().getActive()))
		{
			target.setBrand(brandConverter.convert(source.getBrand()));
		}
		target.setNewArrival(source.getXyzNewArrival());
		target.setMaterial(source.getXyzMaterial());
	}

	public void setBrandConverter(final Converter<BrandModel, BrandData> brandConverter)
	{
		this.brandConverter = brandConverter;
	}
}
