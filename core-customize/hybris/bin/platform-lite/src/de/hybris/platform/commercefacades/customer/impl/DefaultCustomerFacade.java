package de.hybris.platform.commercefacades.customer.impl;

import de.hybris.platform.commercefacades.customer.CustomerFacade;
import de.hybris.platform.commercefacades.user.data.CustomerData;
import de.hybris.platform.commercefacades.user.data.RegisterData;
import de.hybris.platform.commerceservices.customer.CustomerAccountService;
import de.hybris.platform.commerceservices.customer.DuplicateUidException;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.core.model.user.TitleModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.user.UserService;
import java.util.List;
import java.util.Map;

public class DefaultCustomerFacade implements CustomerFacade
{
	private CustomerAccountService customerAccountService;
	private UserService userService;
	private ModelService modelService;
	private FlexibleSearchService flexibleSearchService;
	private Converter<CustomerModel, CustomerData> customerConverter;

	@Override
	public CustomerData register(final RegisterData registerData) throws DuplicateUidException
	{
		final CustomerModel customer = modelService.create(CustomerModel.class);
		customer.setUid(registerData.getLogin());
		customer.setName((registerData.getLastName() + " " + registerData.getFirstName()).trim());
		customer.setTitle(title(registerData.getTitleCode()));
		customerAccountService.register(customer, registerData.getPassword());
		return customerConverter.convert(customer);
	}

	@Override
	public CustomerData getCurrentCustomer()
	{
		return customerConverter.convert((CustomerModel) userService.getCurrentUser());
	}

	@Override
	public CustomerData updateProfile(final CustomerData data)
	{
		final CustomerModel customer = (CustomerModel) userService.getCurrentUser();
		if (data.getFirstName() != null || data.getLastName() != null)
		{
			final CustomerData current = customerConverter.convert(customer);
			customer.setName(((data.getLastName() != null ? data.getLastName() : current.getLastName()) + " "
					+ (data.getFirstName() != null ? data.getFirstName() : current.getFirstName())).trim());
		}
		if (data.getTitleCode() != null)
		{
			customer.setTitle(title(data.getTitleCode()));
		}
		modelService.save(customer);
		return customerConverter.convert(customer);
	}

	private TitleModel title(final String code)
	{
		if (code == null)
		{
			return null;
		}
		final List<TitleModel> list = flexibleSearchService.<TitleModel> search("SELECT {pk} FROM {Title} WHERE {code} = ?c", Map.of("c", code)).getResult();
		return list.isEmpty() ? null : list.get(0);
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

	public void setCustomerConverter(final Converter<CustomerModel, CustomerData> customerConverter)
	{
		this.customerConverter = customerConverter;
	}
}
