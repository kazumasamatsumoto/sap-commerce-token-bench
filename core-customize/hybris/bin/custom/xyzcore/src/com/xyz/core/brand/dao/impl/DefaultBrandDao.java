package com.xyz.core.brand.dao.impl;

import com.xyz.core.brand.dao.BrandDao;
import com.xyz.core.model.BrandModel;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import java.util.List;
import java.util.Map;

public class DefaultBrandDao extends AbstractItemDao implements BrandDao
{
	private static final String FIND_BY_CODE = "SELECT {pk} FROM {" + BrandModel._TYPECODE + "} WHERE {" + BrandModel.CODE + "} = ?code";
	private static final String FIND_ACTIVE = "SELECT {pk} FROM {" + BrandModel._TYPECODE + "} WHERE {" + BrandModel.ACTIVE + "} = true ORDER BY {"
			+ BrandModel.CODE + "}";

	@Override
	public List<BrandModel> findBrandsByCode(final String code)
	{
		return find(FIND_BY_CODE, Map.of("code", code));
	}

	@Override
	public List<BrandModel> findActiveBrands()
	{
		return find(FIND_ACTIVE, Map.of());
	}
}
