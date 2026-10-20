package de.hybris.platform.commerceservices.order;

import de.hybris.platform.core.model.order.CartEntryModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.core.model.user.UserModel;
import java.util.List;

public interface CommerceCartService
{
	/** 追加した結果。statusCode は success / lowStock / noStock。 */
	record Modification(String statusCode, long quantityAdded, long quantity, CartEntryModel entry)
	{
	}

	CartModel createCartForUser(UserModel user);

	/** ログインした人のカートは code、ゲストのカートは guid で探す。無ければ null。 */
	CartModel getCartForCodeAndUser(String code, UserModel user);

	CartModel getCartForGuid(String guid);

	List<CartModel> getCartsForUser(UserModel user);

	Modification addToCart(CartModel cart, ProductModel product, long quantity) throws CommerceCartModificationException;

	Modification updateQuantityForCartEntry(CartModel cart, int entryNumber, long newQuantity) throws CommerceCartModificationException;

	void removeCart(CartModel cart);

	void recalculate(CartModel cart) throws CommerceCartModificationException;
}
