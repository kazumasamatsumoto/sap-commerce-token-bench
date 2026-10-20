package de.hybris.platform.commercefacades.order;

import de.hybris.platform.commercefacades.order.data.CCPaymentInfoData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.DeliveryModeData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.order.InvalidCartException;
import java.util.List;

public interface CheckoutFacade
{
	/** address.id があれば住所録の住所、無ければ新しい住所として登録してから使う。 */
	CartData setDeliveryAddress(String cartId, AddressData address) throws CommerceCartModificationException;

	CartData removeDeliveryAddress(String cartId) throws CommerceCartModificationException;

	List<DeliveryModeData> getSupportedDeliveryModes(String cartId);

	CartData setDeliveryMode(String cartId, String deliveryModeCode) throws CommerceCartModificationException;

	CartData setPaymentDetails(String cartId, CCPaymentInfoData paymentInfo) throws CommerceCartModificationException;

	OrderData placeOrder(String cartId) throws InvalidCartException;
}
