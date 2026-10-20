package de.hybris.platform.servicelayer.search.exceptions;

import de.hybris.platform.servicelayer.exceptions.SystemException;

public class FlexibleSearchException extends SystemException
{
	public FlexibleSearchException(final String message)
	{
		super(message);
	}

	public FlexibleSearchException(final String message, final Throwable cause)
	{
		super(message, cause);
	}
}
