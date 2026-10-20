package de.hybris.platform.commercefacades.order.impl;

import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.CartNotFoundException;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commercefacades.order.data.OrderEntryData;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.order.CommerceCartService;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.product.ProductService;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.user.UserService;
import java.util.List;

public class DefaultCartFacade implements CartFacade
{
	private CommerceCartService commerceCartService;
	private UserService userService;
	private ProductService productService;
	private Converter<CartModel, CartData> cartConverter;
	private Converter<AbstractOrderEntryModel, OrderEntryData> orderEntryConverter;

	/** 今のユーザーのカートを探す。Facade の外(Checkout など)からも使う。 */
	public CartModel loadCart(final String cartId)
	{
		final UserModel user = userService.getCurrentUser();
		final CartModel cart = userService.isAnonymousUser(user) ? commerceCartService.getCartForGuid(cartId)
				: commerceCartService.getCartForCodeAndUser(cartId, user);
		// ゲストは、持ち主が anonymous か「guid|メールアドレス」(ゲストの顧客)のカートだけを使える
		if (cart == null || userService.isAnonymousUser(user) && !userService.isAnonymousUser(cart.getUser()) && !cart.getUser().getUid().contains("|"))
		{
			throw new CartNotFoundException("Cart not found.");
		}
		return cart;
	}

	@Override
	public List<CartData> getCartsForCurrentUser()
	{
		return cartConverter.convertAll(commerceCartService.getCartsForUser(userService.getCurrentUser()));
	}

	@Override
	public CartData createCart()
	{
		return cartConverter.convert(commerceCartService.createCartForUser(userService.getCurrentUser()));
	}

	@Override
	public CartData getCart(final String cartId)
	{
		return cartConverter.convert(loadCart(cartId));
	}

	@Override
	public CartModificationData addToCart(final String cartId, final String productCode, final long quantity) throws CommerceCartModificationException
	{
		final CartModel cart = loadCart(cartId);
		return toData(commerceCartService.addToCart(cart, productService.getProductForCode(productCode), quantity));
	}

	@Override
	public CartModificationData updateCartEntry(final String cartId, final int entryNumber, final long quantity) throws CommerceCartModificationException
	{
		return toData(commerceCartService.updateQuantityForCartEntry(loadCart(cartId), entryNumber, quantity));
	}

	@Override
	public void deleteCart(final String cartId)
	{
		commerceCartService.removeCart(loadCart(cartId));
	}

	private CartModificationData toData(final CommerceCartService.Modification m)
	{
		final CartModificationData d = new CartModificationData();
		d.setStatusCode(m.statusCode());
		d.setQuantityAdded(m.quantityAdded());
		d.setQuantity(m.quantity());
		d.setEntry(m.entry() == null ? null : orderEntryConverter.convert(m.entry()));
		return d;
	}

	public void setCommerceCartService(final CommerceCartService commerceCartService)
	{
		this.commerceCartService = commerceCartService;
	}

	public void setUserService(final UserService userService)
	{
		this.userService = userService;
	}

	public void setProductService(final ProductService productService)
	{
		this.productService = productService;
	}

	public void setCartConverter(final Converter<CartModel, CartData> cartConverter)
	{
		this.cartConverter = cartConverter;
	}

	public void setOrderEntryConverter(final Converter<AbstractOrderEntryModel, OrderEntryData> orderEntryConverter)
	{
		this.orderEntryConverter = orderEntryConverter;
	}
}
