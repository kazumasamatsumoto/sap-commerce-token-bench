package com.xyz.facades.brand;

import com.xyz.facades.brand.data.BrandData;
import java.util.List;

public interface BrandFacade
{
	List<BrandData> getActiveBrands();

	BrandData getBrandForCode(String code);
}
