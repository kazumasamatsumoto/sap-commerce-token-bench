package de.hybris.platform.commerceservices.customer;

import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.search.SearchResult;
import java.util.List;

public interface CustomerAccountService
{
	/** 会員登録。同じ uid があれば DuplicateUidException。 */
	void register(CustomerModel customer, String password) throws DuplicateUidException;

	List<AddressModel> getAddressBookEntries(CustomerModel customer);

	AddressModel getAddressForCode(CustomerModel customer, String code);

	void saveAddressEntry(CustomerModel customer, AddressModel address);

	void deleteAddressEntry(CustomerModel customer, AddressModel address);

	void setDefaultAddressEntry(CustomerModel customer, AddressModel address);

	SearchResult<OrderModel> getOrderList(CustomerModel customer, int currentPage, int pageSize, String sort);

	OrderModel getOrderForCode(CustomerModel customer, String code);

	OrderModel getOrderForGuid(String guid);
}
