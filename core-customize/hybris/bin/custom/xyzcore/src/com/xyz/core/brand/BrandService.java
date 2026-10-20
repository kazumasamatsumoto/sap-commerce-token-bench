package com.xyz.core.brand;

import com.xyz.core.model.BrandModel;
import java.util.List;

public interface BrandService
{
	/** code のブランド。無ければ UnknownIdentifierException。 */
	BrandModel getBrandForCode(String code);

	List<BrandModel> getActiveBrands();
}
