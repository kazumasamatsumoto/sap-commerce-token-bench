package de.hybris.platform.commercefacades.order.impl;

import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.CCPaymentInfoData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.DeliveryModeData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commerceservices.customer.CustomerAccountService;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.order.CommerceCheckoutService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.delivery.DeliveryModeModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.order.DeliveryModeService;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.user.UserService;
import java.util.List;

public class DefaultCheckoutFacade implements CheckoutFacade
{
	private DefaultCartFacade cartFacade;
	private CommerceCheckoutService commerceCheckoutService;
	private CustomerAccountService customerAccountService;
	private DeliveryModeService deliveryModeService;
	private UserService userService;
	private ModelService modelService;
	private Converter<CartModel, CartData> cartConverter;
	private Converter<OrderModel, OrderData> orderConverter;
	private Converter<DeliveryModeModel, DeliveryModeData> deliveryModeConverter;
	private Populator<AddressData, AddressModel> addressReversePopulator;

	@Override
	public CartData setDeliveryAddress(final String cartId, final AddressData address) throws CommerceCartModificationException
	{
		final CartModel cart = cartFacade.loadCart(cartId);
		final AddressModel model;
		if (address.getId() != null && cart.getUser() instanceof CustomerModel customer && !userService.isAnonymousUser(customer))
		{
			model = customerAccountService.getAddressForCode(customer, address.getId());
		}
		else
		{
			model = modelService.create(AddressModel.class);
			addressReversePopulator.populate(address, model);
			model.setOwner(cart.getUser());
			modelService.save(model);
		}
		commerceCheckoutService.setDeliveryAddress(cart, model);
		return cartConverter.convert(cart);
	}

	@Override
	public CartData removeDeliveryAddress(final String cartId) throws CommerceCartModificationException
	{
		final CartModel cart = cartFacade.loadCart(cartId);
		commerceCheckoutService.removeDeliveryAddress(cart);
		return cartConverter.convert(cart);
	}

	@Override
	public List<DeliveryModeData> getSupportedDeliveryModes(final String cartId)
	{
		return deliveryModeConverter.convertAll(deliveryModeService.getSupportedDeliveryModes(cartFacade.loadCart(cartId)));
	}

	@Override
	public CartData setDeliveryMode(final String cartId, final String deliveryModeCode) throws CommerceCartModificationException
	{
		final CartModel cart = cartFacade.loadCart(cartId);
		if (cart.getDeliveryAddress() == null)
		{
			throw new CommerceCartModificationException("Delivery address is not set");
		}
		commerceCheckoutService.setDeliveryMode(cart, deliveryModeService.getDeliveryModeForCode(deliveryModeCode));
		return cartConverter.convert(cart);
	}

	@Override
	public CartData setPaymentDetails(final String cartId, final CCPaymentInfoData paymentInfo) throws CommerceCartModificationException
	{
		final CartModel cart = cartFacade.loadCart(cartId);
		commerceCheckoutService.setPaymentInfo(cart, paymentInfo.getAccountHolderName(), paymentInfo.getCardNumber(), paymentInfo.getExpiryMonth(),
				paymentInfo.getExpiryYear());
		return cartConverter.convert(cart);
	}

	@Override
	public OrderData placeOrder(final String cartId) throws InvalidCartException
	{
		return orderConverter.convert(commerceCheckoutService.placeOrder(cartFacade.loadCart(cartId)));
	}

	public void setCartFacade(final DefaultCartFacade cartFacade)
	{
		this.cartFacade = cartFacade;
	}

	public void setCommerceCheckoutService(final CommerceCheckoutService commerceCheckoutService)
	{
		this.commerceCheckoutService = commerceCheckoutService;
	}

	public void setCustomerAccountService(final CustomerAccountService customerAccountService)
	{
		this.customerAccountService = customerAccountService;
	}

	public void setDeliveryModeService(final DeliveryModeService deliveryModeService)
	{
		this.deliveryModeService = deliveryModeService;
	}

	public void setUserService(final UserService userService)
	{
		this.userService = userService;
	}

	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

	public void setCartConverter(final Converter<CartModel, CartData> cartConverter)
	{
		this.cartConverter = cartConverter;
	}

	public void setOrderConverter(final Converter<OrderModel, OrderData> orderConverter)
	{
		this.orderConverter = orderConverter;
	}

	public void setDeliveryModeConverter(final Converter<DeliveryModeModel, DeliveryModeData> deliveryModeConverter)
	{
		this.deliveryModeConverter = deliveryModeConverter;
	}

	public void setAddressReversePopulator(final Populator<AddressData, AddressModel> addressReversePopulator)
	{
		this.addressReversePopulator = addressReversePopulator;
	}
}
