package de.hybris.platform.commercefacades.order.converters.populators;

import de.hybris.platform.commercefacades.order.data.OrderEntryData;
import de.hybris.platform.commercefacades.product.PriceDataFactory;
import de.hybris.platform.commercefacades.product.data.PriceDataType;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;

public class OrderEntryPopulator implements Populator<AbstractOrderEntryModel, OrderEntryData>
{
	private Converter<ProductModel, ProductData> productConverter;
	private PriceDataFactory priceDataFactory;

	@Override
	public void populate(final AbstractOrderEntryModel source, final OrderEntryData target)
	{
		target.setEntryNumber(source.getEntryNumber());
		target.setQuantity(source.getQuantity());
		target.setProduct(productConverter.convert(source.getProduct()));
		target.setUpdateable(source.getOrder() instanceof CartModel);
		if (source.getOrder() != null && source.getOrder().getCurrency() != null)
		{
			if (source.getBasePrice() != null)
			{
				target.setBasePrice(priceDataFactory.create(PriceDataType.BUY, source.getBasePrice(), source.getOrder().getCurrency()));
			}
			if (source.getTotalPrice() != null)
			{
				target.setTotalPrice(priceDataFactory.create(PriceDataType.BUY, source.getTotalPrice(), source.getOrder().getCurrency()));
			}
		}
	}

	public void setProductConverter(final Converter<ProductModel, ProductData> productConverter)
	{
		this.productConverter = productConverter;
	}

	public void setPriceDataFactory(final PriceDataFactory priceDataFactory)
	{
		this.priceDataFactory = priceDataFactory;
	}
}
