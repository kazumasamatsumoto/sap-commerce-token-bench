package de.hybris.platform.commercefacades.order;

import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.order.data.OrderHistoryData;
import de.hybris.platform.servicelayer.search.SearchResult;

public interface OrderFacade
{
	SearchResult<OrderHistoryData> getPagedOrderHistory(int currentPage, int pageSize, String sort);

	OrderData getOrderDetailsForCode(String code);

	OrderData getOrderDetailsForGUID(String guid);
}
