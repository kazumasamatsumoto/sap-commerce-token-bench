package de.hybris.platform.commercefacades.order.converters.populators;

import de.hybris.platform.commercefacades.order.data.AbstractOrderData;
import de.hybris.platform.commercefacades.order.data.CCPaymentInfoData;
import de.hybris.platform.commercefacades.order.data.DeliveryModeData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.order.data.OrderEntryData;
import de.hybris.platform.commercefacades.product.PriceDataFactory;
import de.hybris.platform.commercefacades.product.data.PriceDataType;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.PrincipalData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.delivery.DeliveryModeModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;

/** カートと注文の共通の項目。カートと注文の Converter の両方で使う。 */
public class AbstractOrderPopulator<T extends AbstractOrderData> implements Populator<AbstractOrderModel, T>
{
	private Converter<AbstractOrderEntryModel, OrderEntryData> orderEntryConverter;
	private Converter<AddressModel, AddressData> addressConverter;
	private Converter<DeliveryModeModel, DeliveryModeData> deliveryModeConverter;
	private PriceDataFactory priceDataFactory;

	@Override
	public void populate(final AbstractOrderModel source, final T target)
	{
		target.setCode(source.getCode());
		target.setGuid(source.getGuid());
		target.setNet(Boolean.TRUE.equals(source.getNet()));
		target.setCalculated(Boolean.TRUE.equals(source.getCalculated()));
		target.setSite(source.getSite() == null ? null : source.getSite().getUid());
		target.setStore(source.getStore() == null ? null : source.getStore().getUid());
		target.setEntries(orderEntryConverter.convertAll(source.getEntries()));
		target.setTotalItems(source.getEntries().size());
		target.setTotalUnitCount(source.getEntries().stream().mapToInt(e -> e.getQuantity().intValue()).sum());
		final CurrencyModel currency = source.getCurrency();
		if (currency != null)
		{
			target.setSubTotal(price(source.getSubtotal(), currency));
			target.setDeliveryCost(source.getDeliveryMode() == null ? null : price(source.getDeliveryCost(), currency));
			target.setTotalTax(price(source.getTotalTax(), currency));
			target.setTotalPrice(price(source.getTotalPrice(), currency));
			target.setTotalPriceWithTax(price(source.getTotalPrice(), currency));
		}
		if (source.getDeliveryAddress() != null)
		{
			target.setDeliveryAddress(addressConverter.convert(source.getDeliveryAddress()));
		}
		if (source.getDeliveryMode() != null)
		{
			target.setDeliveryMode(deliveryModeConverter.convert(source.getDeliveryMode()));
		}
		if (source.getPaymentInfoCode() != null)
		{
			final String[] p = source.getPaymentInfoCode().split("\\|", -1);
			final CCPaymentInfoData info = new CCPaymentInfoData();
			info.setId(source.getCode() + "-payment");
			info.setAccountHolderName(p.length > 0 ? p[0] : null);
			info.setCardNumber(p.length > 1 ? p[1] : null);
			info.setExpiryMonth(p.length > 2 ? p[2] : null);
			info.setExpiryYear(p.length > 3 ? p[3] : null);
			target.setPaymentInfo(info);
		}
		final var user = source instanceof CartModel c ? c.getUser() : source instanceof OrderModel o ? o.getUser() : null;
		if (user != null)
		{
			final PrincipalData principal = new PrincipalData();
			principal.setUid(user.getUid());
			principal.setName(user.getName());
			target.setUser(principal);
		}
		if (source instanceof OrderModel order && target instanceof OrderData data)
		{
			data.setCreated(order.getDate());
			data.setStatus(order.getStatus() == null ? null : order.getStatus().getCode());
			data.setStatusDisplay(order.getStatus() == null ? null : order.getStatus().getCode().toLowerCase());
			data.setGuestCustomer(user == null || "anonymous".equals(user.getUid()));
		}
	}

	private de.hybris.platform.commercefacades.product.data.PriceData price(final Double value, final CurrencyModel currency)
	{
		return priceDataFactory.create(PriceDataType.BUY, value == null ? 0d : value, currency);
	}

	public void setOrderEntryConverter(final Converter<AbstractOrderEntryModel, OrderEntryData> orderEntryConverter)
	{
		this.orderEntryConverter = orderEntryConverter;
	}

	public void setAddressConverter(final Converter<AddressModel, AddressData> addressConverter)
	{
		this.addressConverter = addressConverter;
	}

	public void setDeliveryModeConverter(final Converter<DeliveryModeModel, DeliveryModeData> deliveryModeConverter)
	{
		this.deliveryModeConverter = deliveryModeConverter;
	}

	public void setPriceDataFactory(final PriceDataFactory priceDataFactory)
	{
		this.priceDataFactory = priceDataFactory;
	}
}
