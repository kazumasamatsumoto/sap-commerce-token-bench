package de.hybris.platform.commercefacades.order.converters.populators;

import de.hybris.platform.commercefacades.order.data.OrderHistoryData;
import de.hybris.platform.commercefacades.product.PriceDataFactory;
import de.hybris.platform.commercefacades.product.data.PriceDataType;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.OrderModel;

public class OrderHistoryPopulator implements Populator<OrderModel, OrderHistoryData>
{
	private PriceDataFactory priceDataFactory;

	@Override
	public void populate(final OrderModel source, final OrderHistoryData target)
	{
		target.setCode(source.getCode());
		target.setGuid(source.getGuid());
		target.setPlaced(source.getDate());
		target.setStatus(source.getStatus() == null ? null : source.getStatus().getCode());
		target.setStatusDisplay(target.getStatus() == null ? null : target.getStatus().toLowerCase());
		if (source.getCurrency() != null && source.getTotalPrice() != null)
		{
			target.setTotal(priceDataFactory.create(PriceDataType.BUY, source.getTotalPrice(), source.getCurrency()));
		}
	}

	public void setPriceDataFactory(final PriceDataFactory priceDataFactory)
	{
		this.priceDataFactory = priceDataFactory;
	}
}
