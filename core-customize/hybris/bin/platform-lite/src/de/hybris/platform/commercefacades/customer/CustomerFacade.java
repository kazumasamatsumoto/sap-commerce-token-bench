package de.hybris.platform.commercefacades.customer;

import de.hybris.platform.commercefacades.user.data.CustomerData;
import de.hybris.platform.commercefacades.user.data.RegisterData;
import de.hybris.platform.commerceservices.customer.DuplicateUidException;

public interface CustomerFacade
{
	CustomerData register(RegisterData registerData) throws DuplicateUidException;

	CustomerData getCurrentCustomer();

	CustomerData updateProfile(CustomerData customerData);
}
