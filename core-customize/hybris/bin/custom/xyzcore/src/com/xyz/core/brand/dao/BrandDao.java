package com.xyz.core.brand.dao;

import com.xyz.core.model.BrandModel;
import java.util.List;

public interface BrandDao
{
	List<BrandModel> findBrandsByCode(String code);

	/** 有効なブランドを code の順で。 */
	List<BrandModel> findActiveBrands();
}
