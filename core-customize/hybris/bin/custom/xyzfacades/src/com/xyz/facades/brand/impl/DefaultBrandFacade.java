package com.xyz.facades.brand.impl;

import com.xyz.core.brand.BrandService;
import com.xyz.core.model.BrandModel;
import com.xyz.facades.brand.BrandFacade;
import com.xyz.facades.brand.data.BrandData;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import java.util.List;

public class DefaultBrandFacade implements BrandFacade
{
	private BrandService brandService;
	private Converter<BrandModel, BrandData> brandConverter;

	@Override
	public List<BrandData> getActiveBrands()
	{
		return brandConverter.convertAll(brandService.getActiveBrands());
	}

	@Override
	public BrandData getBrandForCode(final String code)
	{
		return brandConverter.convert(brandService.getBrandForCode(code));
	}

	public void setBrandService(final BrandService brandService)
	{
		this.brandService = brandService;
	}

	public void setBrandConverter(final Converter<BrandModel, BrandData> brandConverter)
	{
		this.brandConverter = brandConverter;
	}
}
