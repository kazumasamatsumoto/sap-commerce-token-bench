package de.hybris.platform.commerceservices.price.impl;

import de.hybris.platform.commerceservices.price.CommercePriceService;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.europe1.model.PriceRowModel;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import java.util.Comparator;

public class DefaultCommercePriceService implements CommercePriceService
{
	private CommonI18NService commonI18NService;

	@Override
	public PriceRowModel getWebPriceForProduct(final ProductModel product)
	{
		final CurrencyModel currency = commonI18NService.getCurrentCurrency();
		return product.getEurope1Prices().stream()
				.filter(p -> p.getCurrency() != null && p.getCurrency().getIsocode().equals(currency.getIsocode()))
				.filter(p -> p.getMinqtd() == null || p.getMinqtd() <= 1)
				.min(Comparator.comparing(PriceRowModel::getPrice))
				.orElse(null);
	}

	public void setCommonI18NService(final CommonI18NService commonI18NService)
	{
		this.commonI18NService = commonI18NService;
	}
}
