package de.hybris.platform.commercefacades.order.converters.populators;

import de.hybris.platform.commercefacades.order.data.DeliveryModeData;
import de.hybris.platform.commercefacades.product.PriceDataFactory;
import de.hybris.platform.commercefacades.product.data.PriceDataType;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.delivery.DeliveryModeModel;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;

public class DeliveryModePopulator implements Populator<DeliveryModeModel, DeliveryModeData>
{
	private PriceDataFactory priceDataFactory;
	private CommonI18NService commonI18NService;

	@Override
	public void populate(final DeliveryModeModel source, final DeliveryModeData target)
	{
		target.setCode(source.getCode());
		target.setName(source.getName());
		target.setDescription(source.getDescription());
		target.setDeliveryCost(priceDataFactory.create(PriceDataType.BUY, source.getCost() == null ? 0d : source.getCost(),
				commonI18NService.getCurrentCurrency()));
	}

	public void setPriceDataFactory(final PriceDataFactory priceDataFactory)
	{
		this.priceDataFactory = priceDataFactory;
	}

	public void setCommonI18NService(final CommonI18NService commonI18NService)
	{
		this.commonI18NService = commonI18NService;
	}
}
