package de.hybris.platform.commercefacades.user.impl;

import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.TitleData;
import de.hybris.platform.commerceservices.customer.CustomerAccountService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.core.model.user.TitleModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.user.UserService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultUserFacade implements UserFacade
{
	private CustomerAccountService customerAccountService;
	private UserService userService;
	private ModelService modelService;
	private FlexibleSearchService flexibleSearchService;
	private Converter<AddressModel, AddressData> addressConverter;
	private Populator<AddressData, AddressModel> addressReversePopulator;

	private CustomerModel current()
	{
		return (CustomerModel) userService.getCurrentUser();
	}

	@Override
	public List<AddressData> getAddressBook()
	{
		final CustomerModel c = current();
		final List<AddressData> list = addressConverter.convertAll(customerAccountService.getAddressBookEntries(c));
		final String defaultId = c.getDefaultShipmentAddress() == null ? null : c.getDefaultShipmentAddress().getPk().toString();
		list.forEach(a -> a.setDefaultAddress(a.getId().equals(defaultId)));
		return list;
	}

	@Override
	public AddressData getAddressForCode(final String code)
	{
		return addressConverter.convert(customerAccountService.getAddressForCode(current(), code));
	}

	@Override
	public AddressData addAddress(final AddressData address)
	{
		final AddressModel model = modelService.create(AddressModel.class);
		addressReversePopulator.populate(address, model);
		customerAccountService.saveAddressEntry(current(), model);
		if (address.isDefaultAddress())
		{
			customerAccountService.setDefaultAddressEntry(current(), model);
		}
		return addressConverter.convert(model);
	}

	@Override
	public AddressData editAddress(final String code, final AddressData address)
	{
		final AddressModel model = customerAccountService.getAddressForCode(current(), code);
		addressReversePopulator.populate(address, model);
		customerAccountService.saveAddressEntry(current(), model);
		return addressConverter.convert(model);
	}

	@Override
	public void removeAddress(final String code)
	{
		customerAccountService.deleteAddressEntry(current(), customerAccountService.getAddressForCode(current(), code));
	}

	@Override
	public List<TitleData> getTitles()
	{
		final List<TitleData> out = new ArrayList<>();
		for (final TitleModel t : flexibleSearchService.<TitleModel> search("SELECT {pk} FROM {Title} ORDER BY {code}", Map.of()).getResult())
		{
			final TitleData d = new TitleData();
			d.setCode(t.getCode());
			d.setName(t.getName());
			out.add(d);
		}
		return out;
	}

	public void setCustomerAccountService(final CustomerAccountService customerAccountService)
	{
		this.customerAccountService = customerAccountService;
	}

	public void setUserService(final UserService userService)
	{
		this.userService = userService;
	}

	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
	{
		this.flexibleSearchService = flexibleSearchService;
	}

	public void setAddressConverter(final Converter<AddressModel, AddressData> addressConverter)
	{
		this.addressConverter = addressConverter;
	}

	public void setAddressReversePopulator(final Populator<AddressData, AddressModel> addressReversePopulator)
	{
		this.addressReversePopulator = addressReversePopulator;
	}
}
