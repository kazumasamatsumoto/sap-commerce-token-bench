package de.hybris.platform.servicelayer.exceptions;

/** 1 つだけのはずが複数見つかったとき。 */
public class AmbiguousIdentifierException extends SystemException
{
	public AmbiguousIdentifierException(final String message)
	{
		super(message);
	}
}
