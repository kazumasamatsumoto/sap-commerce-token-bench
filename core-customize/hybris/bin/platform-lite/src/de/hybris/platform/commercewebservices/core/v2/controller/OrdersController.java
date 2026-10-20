package de.hybris.platform.commercewebservices.core.v2.controller;

import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.OrderHistoryData;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderHistoryListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderHistoryWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.servicelayer.search.SearchResult;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/{baseSiteId}")
public class OrdersController extends BaseController
{
	@Resource(name = "orderFacade")
	private OrderFacade orderFacade;

	@Resource(name = "checkoutFacade")
	private CheckoutFacade checkoutFacade;

	/** POST /{baseSiteId}/users/{userId}/orders?cartId= … 注文を確定する。 */
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_ANONYMOUS", "ROLE_CLIENT" })
	@PostMapping("/users/{userId}/orders")
	@ResponseStatus(HttpStatus.CREATED)
	public OrderWsDTO placeOrder(@PathVariable final String userId, @RequestParam final String cartId,
			@RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) throws InvalidCartException
	{
		return getDataMapper().map(checkoutFacade.placeOrder(cartId), OrderWsDTO.class, fields);
	}

	/** GET /{baseSiteId}/users/{userId}/orders?currentPage=&pageSize=&sort=byDate|byOrderNumber */
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT" })
	@GetMapping("/users/{userId}/orders")
	public OrderHistoryListWsDTO getOrders(@PathVariable final String userId, @RequestParam(defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage,
			@RequestParam(defaultValue = DEFAULT_PAGE_SIZE) final int pageSize, @RequestParam(defaultValue = "byDate") final String sort,
			@RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final int size = pageSize(pageSize);
		final SearchResult<OrderHistoryData> result = orderFacade.getPagedOrderHistory(currentPage, size, sort);
		final OrderHistoryListWsDTO list = new OrderHistoryListWsDTO();
		list.setOrders(result.getResult().stream().map(o -> getDataMapper().map(o, OrderHistoryWsDTO.class, fields)).toList());
		list.setPagination(pagination(result, currentPage, size, sort));
		return list;
	}

	/** GET /{baseSiteId}/users/{userId}/orders/{code} */
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT" })
	@GetMapping("/users/{userId}/orders/{code}")
	public OrderWsDTO getOrder(@PathVariable final String userId, @PathVariable final String code,
			@RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		return getDataMapper().map(orderFacade.getOrderDetailsForCode(code), OrderWsDTO.class, fields);
	}

	/** GET /{baseSiteId}/orders/{guid} … ゲストの注文(guid を知っている人だけ見られる)。 */
	@GetMapping("/orders/{guid}")
	public OrderWsDTO getGuestOrder(@PathVariable final String guid, @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		return getDataMapper().map(orderFacade.getOrderDetailsForGUID(guid), OrderWsDTO.class, fields);
	}
}
