package de.hybris.platform.servicelayer.dto.converter;

import de.hybris.platform.servicelayer.exceptions.SystemException;

public class ConversionException extends SystemException
{
	public ConversionException(final String message)
	{
		super(message);
	}

	public ConversionException(final String message, final Throwable cause)
	{
		super(message, cause);
	}
}
