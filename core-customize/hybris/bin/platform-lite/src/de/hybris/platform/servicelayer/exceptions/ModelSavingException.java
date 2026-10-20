package de.hybris.platform.servicelayer.exceptions;

public class ModelSavingException extends SystemException
{
	public ModelSavingException(final String message)
	{
		super(message);
	}

	public ModelSavingException(final String message, final Throwable cause)
	{
		super(message, cause);
	}
}
