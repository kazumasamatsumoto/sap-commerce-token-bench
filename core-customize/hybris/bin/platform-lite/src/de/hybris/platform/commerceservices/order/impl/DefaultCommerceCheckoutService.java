package de.hybris.platform.commerceservices.order.impl;

import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.order.CommerceCartService;
import de.hybris.platform.commerceservices.order.CommerceCheckoutService;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderEntryModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.delivery.DeliveryModeModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.order.DeliveryModeService;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DefaultCommerceCheckoutService extends AbstractItemDao implements CommerceCheckoutService
{
	private CommerceCartService commerceCartService;
	private DeliveryModeService deliveryModeService;

	@Override
	public void setDeliveryAddress(final CartModel cart, final AddressModel address) throws CommerceCartModificationException
	{
		cart.setDeliveryAddress(address);
		getModelService().save(cart);
		commerceCartService.recalculate(cart);
	}

	@Override
	public void removeDeliveryAddress(final CartModel cart) throws CommerceCartModificationException
	{
		cart.setDeliveryAddress(null);
		cart.setDeliveryMode(null);
		getModelService().save(cart);
		commerceCartService.recalculate(cart);
	}

	@Override
	public void setDeliveryMode(final CartModel cart, final DeliveryModeModel deliveryMode) throws CommerceCartModificationException
	{
		if (deliveryModeService.getSupportedDeliveryModes(cart).stream().noneMatch(m -> m.getCode().equals(deliveryMode.getCode())))
		{
			throw new CommerceCartModificationException("Delivery Mode [" + deliveryMode.getCode() + "] is not supported for the current cart");
		}
		cart.setDeliveryMode(deliveryMode);
		getModelService().save(cart);
		commerceCartService.recalculate(cart);
	}

	@Override
	public void setPaymentInfo(final CartModel cart, final String accountHolderName, final String cardNumber, final String expiryMonth,
			final String expiryYear) throws CommerceCartModificationException
	{
		if (cardNumber == null || !cardNumber.replaceAll("\\s", "").matches("\\d{12,19}"))
		{
			throw new CommerceCartModificationException("Invalid card number");
		}
		final String digits = cardNumber.replaceAll("\\s", "");
		cart.setPaymentInfoCode(String.join("|", accountHolderName == null ? "" : accountHolderName, "************" + digits.substring(digits.length() - 4),
				expiryMonth == null ? "" : expiryMonth, expiryYear == null ? "" : expiryYear));
		getModelService().save(cart);
	}

	@Override
	public OrderModel placeOrder(final CartModel cart) throws InvalidCartException
	{
		if (cart.getEntries().isEmpty())
		{
			throw new InvalidCartException("Cart is empty");
		}
		if (cart.getDeliveryAddress() == null)
		{
			throw new InvalidCartException("Delivery address is not set");
		}
		if (cart.getDeliveryMode() == null)
		{
			throw new InvalidCartException("Delivery mode is not set");
		}
		if (cart.getPaymentInfoCode() == null)
		{
			throw new InvalidCartException("Payment info is not set");
		}
		final OrderModel order = getModelService().create(OrderModel.class);
		order.setCode(nextOrderCode());
		order.setGuid(UUID.randomUUID().toString());
		order.setDate(new Date());
		order.setUser(cart.getUser());
		order.setSite(cart.getSite());
		order.setStore(cart.getStore());
		order.setCurrency(cart.getCurrency());
		order.setStatus(OrderStatus.CREATED);
		order.setNet(cart.getNet());
		order.setCalculated(Boolean.TRUE);
		order.setSubtotal(cart.getSubtotal());
		order.setDeliveryCost(cart.getDeliveryCost());
		order.setTotalTax(cart.getTotalTax());
		order.setTotalPrice(cart.getTotalPrice());
		order.setDeliveryAddress(cart.getDeliveryAddress());
		order.setPaymentAddress(cart.getPaymentAddress());
		order.setDeliveryMode(cart.getDeliveryMode());
		order.setPaymentInfoCode(cart.getPaymentInfoCode());
		final List<AbstractOrderEntryModel> entries = new ArrayList<>();
		for (final AbstractOrderEntryModel ce : cart.getEntries())
		{
			final OrderEntryModel oe = getModelService().create(OrderEntryModel.class);
			oe.setEntryNumber(ce.getEntryNumber());
			oe.setProduct(ce.getProduct());
			oe.setQuantity(ce.getQuantity());
			oe.setBasePrice(ce.getBasePrice());
			oe.setTotalPrice(ce.getTotalPrice());
			oe.setUnit(ce.getUnit());
			entries.add(oe);
		}
		order.setEntries(entries);
		getModelService().save(order);
		commerceCartService.removeCart(cart);
		return order;
	}

	private String nextOrderCode()
	{
		final de.hybris.platform.servicelayer.search.FlexibleSearchQuery q = new de.hybris.platform.servicelayer.search.FlexibleSearchQuery(
				"SELECT count({pk}) FROM {Order}", Map.of());
		q.setResultClassList(List.of(Long.class));
		final long count = getFlexibleSearchService().<Long> search(q).getResult().get(0);
		// サンプルデータの order-000001.. とぶつからないよう、画面からの注文は 1 から始めて頭に w を付ける
		return "order-w" + String.format("%06d", count + 1) + "-" + UUID.randomUUID().toString().substring(0, 4);
	}

	public void setCommerceCartService(final CommerceCartService commerceCartService)
	{
		this.commerceCartService = commerceCartService;
	}

	public void setDeliveryModeService(final DeliveryModeService deliveryModeService)
	{
		this.deliveryModeService = deliveryModeService;
	}
}
