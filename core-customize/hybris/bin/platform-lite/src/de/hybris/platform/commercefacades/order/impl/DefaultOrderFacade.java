package de.hybris.platform.commercefacades.order.impl;

import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.order.data.OrderHistoryData;
import de.hybris.platform.commerceservices.customer.CustomerAccountService;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.servicelayer.search.impl.DefaultFlexibleSearchService.DefaultSearchResult;
import de.hybris.platform.servicelayer.user.UserService;

public class DefaultOrderFacade implements OrderFacade
{
	private CustomerAccountService customerAccountService;
	private UserService userService;
	private Converter<OrderModel, OrderData> orderConverter;
	private Converter<OrderModel, OrderHistoryData> orderHistoryConverter;

	@Override
	public SearchResult<OrderHistoryData> getPagedOrderHistory(final int currentPage, final int pageSize, final String sort)
	{
		final SearchResult<OrderModel> r = customerAccountService.getOrderList((CustomerModel) userService.getCurrentUser(), currentPage, pageSize, sort);
		return new DefaultSearchResult<>(orderHistoryConverter.convertAll(r.getResult()), r.getTotalCount(), r.getRequestedStart(), r.getRequestedCount());
	}

	@Override
	public OrderData getOrderDetailsForCode(final String code)
	{
		return orderConverter.convert(customerAccountService.getOrderForCode((CustomerModel) userService.getCurrentUser(), code));
	}

	@Override
	public OrderData getOrderDetailsForGUID(final String guid)
	{
		return orderConverter.convert(customerAccountService.getOrderForGuid(guid));
	}

	public void setCustomerAccountService(final CustomerAccountService customerAccountService)
	{
		this.customerAccountService = customerAccountService;
	}

	public void setUserService(final UserService userService)
	{
		this.userService = userService;
	}

	public void setOrderConverter(final Converter<OrderModel, OrderData> orderConverter)
	{
		this.orderConverter = orderConverter;
	}

	public void setOrderHistoryConverter(final Converter<OrderModel, OrderHistoryData> orderHistoryConverter)
	{
		this.orderHistoryConverter = orderHistoryConverter;
	}
}
