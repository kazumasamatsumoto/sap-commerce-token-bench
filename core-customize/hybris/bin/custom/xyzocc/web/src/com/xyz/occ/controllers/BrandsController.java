package com.xyz.occ.controllers;

import com.xyz.facades.brand.BrandFacade;
import com.xyz.occ.dto.brand.BrandListWsDTO;
import com.xyz.occ.dto.brand.BrandWsDTO;
import de.hybris.platform.commercewebservices.core.v2.controller.BaseController;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 自社の OCC 端点: ブランド。 */
@RestController
@RequestMapping("/{baseSiteId}/brands")
public class BrandsController extends BaseController
{
	@Resource(name = "brandFacade")
	private BrandFacade brandFacade;

	/** GET /{baseSiteId}/brands … 有効なブランドの一覧。 */
	@GetMapping
	public BrandListWsDTO getBrands(@RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final BrandListWsDTO list = new BrandListWsDTO();
		list.setBrands(brandFacade.getActiveBrands().stream().map(b -> getDataMapper().map(b, BrandWsDTO.class, fields)).toList());
		return list;
	}

	/** GET /{baseSiteId}/brands/{brandCode} */
	@GetMapping("/{brandCode}")
	public BrandWsDTO getBrand(@PathVariable final String brandCode, @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		return getDataMapper().map(brandFacade.getBrandForCode(brandCode), BrandWsDTO.class, fields);
	}
}
