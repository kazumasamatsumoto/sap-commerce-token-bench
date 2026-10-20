package de.hybris.platform.commerceservices.order;

import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.delivery.DeliveryModeModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.order.InvalidCartException;

public interface CommerceCheckoutService
{
	void setDeliveryAddress(CartModel cart, AddressModel address) throws CommerceCartModificationException;

	void removeDeliveryAddress(CartModel cart) throws CommerceCartModificationException;

	void setDeliveryMode(CartModel cart, DeliveryModeModel deliveryMode) throws CommerceCartModificationException;

	/** 支払い情報(疑似。カード番号の下 4 桁だけを持つ)。 */
	void setPaymentInfo(CartModel cart, String accountHolderName, String cardNumber, String expiryMonth, String expiryYear)
			throws CommerceCartModificationException;

	/** カートから注文を作り、カートを消す。足りないものがあれば InvalidCartException。 */
	OrderModel placeOrder(CartModel cart) throws InvalidCartException;
}
