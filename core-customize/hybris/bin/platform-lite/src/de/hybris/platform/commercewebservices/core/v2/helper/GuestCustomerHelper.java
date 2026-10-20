package de.hybris.platform.commercewebservices.core.v2.helper;

import de.hybris.platform.commercefacades.order.CartNotFoundException;
import de.hybris.platform.commerceservices.order.CommerceCartService;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.user.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/** ゲストの注文のため、「guid|メールアドレス」の uid の顧客を作ってカートに付ける(本物と同じ uid の形)。 */
@Component("guestCustomerHelper")
public class GuestCustomerHelper
{
	@Resource(name = "commerceCartService")
	private CommerceCartService commerceCartService;

	@Resource(name = "userService")
	private UserService userService;

	@Resource(name = "modelService")
	private ModelService modelService;

	public void assignGuest(final String cartGuid, final String email)
	{
		final CartModel cart = commerceCartService.getCartForGuid(cartGuid);
		if (cart == null || !isGuestOrAnonymous(cart.getUser().getUid()))
		{
			throw new CartNotFoundException("Cart not found.");
		}
		final CustomerModel guest = modelService.create(CustomerModel.class);
		guest.setUid(cart.getGuid() + "|" + email.toLowerCase());
		guest.setName("guest");
		guest.setCustomerID(cart.getGuid());
		modelService.save(guest);
		cart.setUser(guest);
		modelService.save(cart);
	}

	public static boolean isGuestOrAnonymous(final String uid)
	{
		return "anonymous".equals(uid) || uid.contains("|");
	}
}
