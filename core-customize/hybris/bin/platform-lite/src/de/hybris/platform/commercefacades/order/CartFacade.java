package de.hybris.platform.commercefacades.order;

import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import java.util.List;

/**
 * カートの窓口。cartId はログインした人ならカートの code、ゲスト(anonymous)なら guid。
 * 見つからないカートは CartNotFoundException(OCC では 400 の CartError)。
 */
public interface CartFacade
{
	List<CartData> getCartsForCurrentUser();

	CartData createCart();

	CartData getCart(String cartId);

	CartModificationData addToCart(String cartId, String productCode, long quantity) throws CommerceCartModificationException;

	CartModificationData updateCartEntry(String cartId, int entryNumber, long quantity) throws CommerceCartModificationException;

	void deleteCart(String cartId);
}
