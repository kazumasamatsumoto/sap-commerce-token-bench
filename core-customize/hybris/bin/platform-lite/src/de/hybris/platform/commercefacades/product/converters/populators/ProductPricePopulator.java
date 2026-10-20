package de.hybris.platform.commercefacades.product.converters.populators;

import de.hybris.platform.commercefacades.product.PriceDataFactory;
import de.hybris.platform.commercefacades.product.data.PriceDataType;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commerceservices.price.CommercePriceService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.europe1.model.PriceRowModel;

/** 今の通貨の価格。価格が無い商品は purchasable を false にする。 */
public class ProductPricePopulator implements Populator<ProductModel, ProductData>
{
	private CommercePriceService commercePriceService;
	private PriceDataFactory priceDataFactory;

	@Override
	public void populate(final ProductModel source, final ProductData target)
	{
		final PriceRowModel price = commercePriceService.getWebPriceForProduct(source);
		if (price == null)
		{
			target.setPurchasable(Boolean.FALSE);
			return;
		}
		target.setPrice(priceDataFactory.create(PriceDataType.BUY, price.getPrice(), price.getCurrency()));
	}

	public void setCommercePriceService(final CommercePriceService commercePriceService)
	{
		this.commercePriceService = commercePriceService;
	}

	public void setPriceDataFactory(final PriceDataFactory priceDataFactory)
	{
		this.priceDataFactory = priceDataFactory;
	}
}
