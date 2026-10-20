package de.hybris.platform.order.impl;

import de.hybris.platform.commerceservices.price.CommercePriceService;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.europe1.model.PriceRowModel;
import de.hybris.platform.order.CalculationService;
import de.hybris.platform.order.exceptions.CalculationException;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.model.ModelService;

/**
 * 価格は税込み(gross)として扱い、消費税は内税で出す(税額 = 合計 × 税率 / (100 + 税率))。
 */
public class DefaultCalculationService implements CalculationService
{
	private ModelService modelService;
	private CommercePriceService commercePriceService;
	private CommonI18NService commonI18NService;
	private double taxRatePercent = 10d;

	@Override
	public void calculate(final AbstractOrderModel order) throws CalculationException
	{
		final int digits = order.getCurrency() == null || order.getCurrency().getDigits() == null ? 0 : order.getCurrency().getDigits();
		double subtotal = 0;
		for (final AbstractOrderEntryModel entry : order.getEntries())
		{
			final PriceRowModel price = commercePriceService.getWebPriceForProduct(entry.getProduct());
			if (price == null)
			{
				throw new CalculationException("No price for product " + entry.getProduct().getCode() + " in currency "
						+ commonI18NService.getCurrentCurrency().getIsocode());
			}
			entry.setBasePrice(price.getPrice());
			entry.setTotalPrice(commonI18NService.roundCurrency(price.getPrice() * entry.getQuantity(), digits));
			subtotal += entry.getTotalPrice();
			modelService.save(entry);
		}
		final double delivery = order.getDeliveryMode() == null || order.getDeliveryMode().getCost() == null ? 0d : order.getDeliveryMode().getCost();
		final double total = subtotal + delivery;
		order.setSubtotal(commonI18NService.roundCurrency(subtotal, digits));
		order.setDeliveryCost(delivery);
		order.setTotalPrice(commonI18NService.roundCurrency(total, digits));
		order.setTotalTax(commonI18NService.roundCurrency(total * taxRatePercent / (100d + taxRatePercent), digits));
		order.setNet(Boolean.FALSE);
		order.setCalculated(Boolean.TRUE);
		modelService.save(order);
	}

	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

	public void setCommercePriceService(final CommercePriceService commercePriceService)
	{
		this.commercePriceService = commercePriceService;
	}

	public void setCommonI18NService(final CommonI18NService commonI18NService)
	{
		this.commonI18NService = commonI18NService;
	}

	public void setTaxRatePercent(final double taxRatePercent)
	{
		this.taxRatePercent = taxRatePercent;
	}
}
