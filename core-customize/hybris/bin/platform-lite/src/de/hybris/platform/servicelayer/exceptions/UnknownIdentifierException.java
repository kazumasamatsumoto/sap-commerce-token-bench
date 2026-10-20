package de.hybris.platform.servicelayer.exceptions;

/** 探したものが無いとき。OCC では 400 の UnknownIdentifierError になる。 */
public class UnknownIdentifierException extends SystemException
{
	public UnknownIdentifierException(final String message)
	{
		super(message);
	}
}
