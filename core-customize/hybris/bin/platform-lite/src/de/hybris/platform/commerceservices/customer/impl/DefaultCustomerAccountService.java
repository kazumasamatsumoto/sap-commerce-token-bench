package de.hybris.platform.commerceservices.customer.impl;

import de.hybris.platform.commerceservices.customer.CustomerAccountService;
import de.hybris.platform.commerceservices.customer.DuplicateUidException;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.security.PrincipalGroupModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.site.BaseSiteService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DefaultCustomerAccountService extends AbstractItemDao implements CustomerAccountService
{
	private UserService userService;
	private BaseSiteService baseSiteService;

	@Override
	public void register(final CustomerModel customer, final String password) throws DuplicateUidException
	{
		customer.setUid(customer.getUid().toLowerCase());
		if (userService.isUserExisting(customer.getUid()))
		{
			throw new DuplicateUidException("User with email " + customer.getUid() + " already exists.");
		}
		customer.setCustomerID(UUID.randomUUID().toString());
		userService.setPassword(customer, password);
		final Set<PrincipalGroupModel> groups = new HashSet<>(customer.getGroups());
		groups.add(userService.getUserGroupForUID("customergroup"));
		customer.setGroups(groups);
		getModelService().save(customer);
	}

	@Override
	public List<AddressModel> getAddressBookEntries(final CustomerModel customer)
	{
		return find("SELECT {pk} FROM {Address} WHERE {owner} = ?owner ORDER BY {pk}", Map.of("owner", customer));
	}

	@Override
	public AddressModel getAddressForCode(final CustomerModel customer, final String code)
	{
		for (final AddressModel a : getAddressBookEntries(customer))
		{
			if (a.getPk().toString().equals(code))
			{
				return a;
			}
		}
		throw new UnknownIdentifierException("Address not found for the current user");
	}

	@Override
	public void saveAddressEntry(final CustomerModel customer, final AddressModel address)
	{
		address.setOwner(customer);
		getModelService().save(address);
		if (customer.getDefaultShipmentAddress() == null)
		{
			setDefaultAddressEntry(customer, address);
		}
	}

	@Override
	public void deleteAddressEntry(final CustomerModel customer, final AddressModel address)
	{
		if (customer.getDefaultShipmentAddress() != null && customer.getDefaultShipmentAddress().getPk().equals(address.getPk()))
		{
			customer.setDefaultShipmentAddress(null);
			getModelService().save(customer);
		}
		getModelService().remove(address);
	}

	@Override
	public void setDefaultAddressEntry(final CustomerModel customer, final AddressModel address)
	{
		customer.setDefaultShipmentAddress(address);
		getModelService().save(customer);
	}

	@Override
	public SearchResult<OrderModel> getOrderList(final CustomerModel customer, final int currentPage, final int pageSize, final String sort)
	{
		final String order = "byOrderNumber".equals(sort) ? " ORDER BY {code} DESC" : " ORDER BY {date} DESC, {code} DESC";
		final FlexibleSearchQuery q = new FlexibleSearchQuery("SELECT {pk} FROM {Order} WHERE {user} = ?user AND {site} = ?site" + order,
				Map.of("user", customer, "site", baseSiteService.getCurrentBaseSite()));
		q.setStart(currentPage * pageSize);
		q.setCount(pageSize);
		q.setNeedTotal(true);
		return getFlexibleSearchService().search(q);
	}

	@Override
	public OrderModel getOrderForCode(final CustomerModel customer, final String code)
	{
		final List<OrderModel> list = find("SELECT {pk} FROM {Order} WHERE {code} = ?code AND {user} = ?user", Map.of("code", code, "user", customer));
		if (list.isEmpty())
		{
			throw new ModelNotFoundException("Order with code " + code + " not found for current user in current BaseStore");
		}
		return list.get(0);
	}

	@Override
	public OrderModel getOrderForGuid(final String guid)
	{
		final List<OrderModel> list = find("SELECT {pk} FROM {Order} WHERE {guid} = ?guid", Map.of("guid", guid));
		if (list.isEmpty())
		{
			throw new ModelNotFoundException("Order with guid " + guid + " not found");
		}
		return list.get(0);
	}

	public void setUserService(final UserService userService)
	{
		this.userService = userService;
	}

	public void setBaseSiteService(final BaseSiteService baseSiteService)
	{
		this.baseSiteService = baseSiteService;
	}
}
