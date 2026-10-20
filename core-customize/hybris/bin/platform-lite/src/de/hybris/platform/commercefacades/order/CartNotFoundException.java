package de.hybris.platform.commercefacades.order;

import de.hybris.platform.servicelayer.exceptions.SystemException;

public class CartNotFoundException extends SystemException
{
	public CartNotFoundException(final String message)
	{
		super(message);
	}
}
