package de.hybris.platform.commercewebservices.core.v2.controller;

import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.data.CCPaymentInfoData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commercewebservices.core.exceptions.RequestParameterException;
import de.hybris.platform.commercewebservices.core.v2.helper.GuestCustomerHelper;
import de.hybris.platform.commercewebservicescommons.dto.order.CartListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.CartModificationWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.CartWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.DeliveryModeListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.DeliveryModeWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderEntryListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderEntryWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.user.AddressWsDTO;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * カートとチェックアウト。{userId} は current(ログインした人)か anonymous(ゲスト。{cartId} は guid)。
 */
@RestController
@RequestMapping("/{baseSiteId}/users/{userId}/carts")
@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_ANONYMOUS", "ROLE_CLIENT" })
public class CartsController extends BaseController
{
	@Resource(name = "cartFacade")
	private CartFacade cartFacade;

	@Resource(name = "checkoutFacade")
	private CheckoutFacade checkoutFacade;

	@Resource(name = "guestCustomerHelper")
	private GuestCustomerHelper guestCustomerHelper;

	/** GET .../carts */
	@GetMapping
	public CartListWsDTO getCarts(@RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final CartListWsDTO list = new CartListWsDTO();
		list.setCarts(cartFacade.getCartsForCurrentUser().stream().map(c -> getDataMapper().map(c, CartWsDTO.class, fields)).toList());
		return list;
	}

	/** POST .../carts … 新しいカート。 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CartWsDTO createCart(@RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		return getDataMapper().map(cartFacade.createCart(), CartWsDTO.class, fields);
	}

	/** GET .../carts/{cartId} */
	@GetMapping("/{cartId}")
	public CartWsDTO getCart(@PathVariable final String cartId, @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		return getDataMapper().map(cartFacade.getCart(cartId), CartWsDTO.class, fields);
	}

	/** DELETE .../carts/{cartId} */
	@DeleteMapping("/{cartId}")
	@ResponseStatus(HttpStatus.OK)
	public void deleteCart(@PathVariable final String cartId)
	{
		cartFacade.deleteCart(cartId);
	}

	/** GET .../carts/{cartId}/entries */
	@GetMapping("/{cartId}/entries")
	public OrderEntryListWsDTO getCartEntries(@PathVariable final String cartId, @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final CartData cart = cartFacade.getCart(cartId);
		final OrderEntryListWsDTO list = new OrderEntryListWsDTO();
		list.setOrderEntries(cart.getEntries().stream().map(e -> getDataMapper().map(e, OrderEntryWsDTO.class, fields)).toList());
		return list;
	}

	/** POST .../carts/{cartId}/entries … 本体は {"product":{"code":"XYZ-000001"},"quantity":1} */
	@PostMapping("/{cartId}/entries")
	public CartModificationWsDTO addCartEntry(@PathVariable final String cartId, @RequestBody final OrderEntryWsDTO entry,
			@RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) throws CommerceCartModificationException
	{
		if (entry.getProduct() == null || entry.getProduct().getCode() == null)
		{
			throw new RequestParameterException("product.code is required", "product.code");
		}
		final long quantity = entry.getQuantity() == null ? 1 : entry.getQuantity();
		return getDataMapper().map(cartFacade.addToCart(cartId, entry.getProduct().getCode(), quantity), CartModificationWsDTO.class, fields);
	}

	/** PATCH .../carts/{cartId}/entries/{entryNumber} … 本体は {"quantity":2}(0 なら消す) */
	@PatchMapping("/{cartId}/entries/{entryNumber}")
	public CartModificationWsDTO updateCartEntry(@PathVariable final String cartId, @PathVariable final int entryNumber,
			@RequestBody final OrderEntryWsDTO entry, @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws CommerceCartModificationException
	{
		if (entry.getQuantity() == null || entry.getQuantity() < 0)
		{
			throw new RequestParameterException("quantity must not be negative", "quantity");
		}
		return getDataMapper().map(cartFacade.updateCartEntry(cartId, entryNumber, entry.getQuantity()), CartModificationWsDTO.class, fields);
	}

	/** DELETE .../carts/{cartId}/entries/{entryNumber} */
	@DeleteMapping("/{cartId}/entries/{entryNumber}")
	@ResponseStatus(HttpStatus.OK)
	public void removeCartEntry(@PathVariable final String cartId, @PathVariable final int entryNumber) throws CommerceCartModificationException
	{
		cartFacade.updateCartEntry(cartId, entryNumber, 0);
	}

	/** PUT .../carts/{cartId}/addresses/delivery?addressId= か、本体に住所(新しい住所) */
	@PutMapping("/{cartId}/addresses/delivery")
	@ResponseStatus(HttpStatus.OK)
	public void setDeliveryAddress(@PathVariable final String cartId, @RequestParam(required = false) final String addressId,
			@RequestBody(required = false) final AddressWsDTO address) throws CommerceCartModificationException
	{
		final AddressData data;
		if (addressId != null)
		{
			data = new AddressData();
			data.setId(addressId);
		}
		else if (address != null)
		{
			AddressController.validate(address);
			data = getDataMapper().mapToData(address, AddressData.class);
			data.setId(null);
		}
		else
		{
			throw new RequestParameterException("addressId or an address in the body is required", "addressId");
		}
		checkoutFacade.setDeliveryAddress(cartId, data);
	}

	/** DELETE .../carts/{cartId}/addresses/delivery */
	@DeleteMapping("/{cartId}/addresses/delivery")
	@ResponseStatus(HttpStatus.OK)
	public void removeDeliveryAddress(@PathVariable final String cartId) throws CommerceCartModificationException
	{
		checkoutFacade.removeDeliveryAddress(cartId);
	}

	/** GET .../carts/{cartId}/deliverymodes */
	@GetMapping("/{cartId}/deliverymodes")
	public DeliveryModeListWsDTO getSupportedDeliveryModes(@PathVariable final String cartId,
			@RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final DeliveryModeListWsDTO list = new DeliveryModeListWsDTO();
		list.setDeliveryModes(
				checkoutFacade.getSupportedDeliveryModes(cartId).stream().map(m -> getDataMapper().map(m, DeliveryModeWsDTO.class, fields)).toList());
		return list;
	}

	/** PUT .../carts/{cartId}/deliverymode?deliveryModeId= */
	@PutMapping("/{cartId}/deliverymode")
	@ResponseStatus(HttpStatus.OK)
	public void setDeliveryMode(@PathVariable final String cartId, @RequestParam final String deliveryModeId) throws CommerceCartModificationException
	{
		checkoutFacade.setDeliveryMode(cartId, deliveryModeId);
	}

	/** GET .../carts/{cartId}/deliverymode */
	@GetMapping("/{cartId}/deliverymode")
	public DeliveryModeWsDTO getDeliveryMode(@PathVariable final String cartId, @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final CartData cart = cartFacade.getCart(cartId);
		return cart.getDeliveryMode() == null ? new DeliveryModeWsDTO() : getDataMapper().map(cart.getDeliveryMode(), DeliveryModeWsDTO.class, fields);
	}

	/** POST .../carts/{cartId}/paymentdetails … 本体は {"accountHolderName":"","cardNumber":"4111111111111111","expiryMonth":"12","expiryYear":"2030"} */
	@PostMapping("/{cartId}/paymentdetails")
	@ResponseStatus(HttpStatus.CREATED)
	public PaymentDetailsWsDTO addPaymentDetails(@PathVariable final String cartId, @RequestBody final PaymentDetailsWsDTO payment,
			@RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) throws CommerceCartModificationException
	{
		final CCPaymentInfoData data = new CCPaymentInfoData();
		data.setAccountHolderName(payment.getAccountHolderName());
		data.setCardNumber(payment.getCardNumber());
		data.setExpiryMonth(payment.getExpiryMonth());
		data.setExpiryYear(payment.getExpiryYear());
		final CartData cart = checkoutFacade.setPaymentDetails(cartId, data);
		return getDataMapper().map(cart.getPaymentInfo(), PaymentDetailsWsDTO.class, fields);
	}

	/** PUT .../carts/{cartId}/email?email= … ゲストのカートにメールアドレスを付ける(ゲストの注文に要る)。 */
	@PutMapping("/{cartId}/email")
	@ResponseStatus(HttpStatus.OK)
	public void setCartEmail(@PathVariable final String cartId, @RequestParam final String email)
	{
		if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
		{
			throw new RequestParameterException("email is not valid", "email");
		}
		guestCustomerHelper.assignGuest(cartId, email);
	}

}
