package com.xyz.core.brand.impl;

import com.xyz.core.brand.BrandService;
import com.xyz.core.brand.dao.BrandDao;
import com.xyz.core.model.BrandModel;
import de.hybris.platform.servicelayer.exceptions.AmbiguousIdentifierException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import java.util.List;
import java.util.Objects;

public class DefaultBrandService implements BrandService
{
	private BrandDao brandDao;

	@Override
	public BrandModel getBrandForCode(final String code)
	{
		Objects.requireNonNull(code, "code must not be null");
		final List<BrandModel> brands = brandDao.findBrandsByCode(code);
		if (brands.isEmpty())
		{
			throw new UnknownIdentifierException("Brand with code '" + code + "' not found!");
		}
		if (brands.size() > 1)
		{
			throw new AmbiguousIdentifierException("Brand code '" + code + "' is not unique, " + brands.size() + " brands found!");
		}
		return brands.get(0);
	}

	@Override
	public List<BrandModel> getActiveBrands()
	{
		return brandDao.findActiveBrands();
	}

	public void setBrandDao(final BrandDao brandDao)
	{
		this.brandDao = brandDao;
	}
}
