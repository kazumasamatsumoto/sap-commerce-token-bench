package de.hybris.platform.commercewebservices.core.exceptions;

/** URL の中の値(サイトなど)が無いとき。OCC では 400 の InvalidResourceError。 */
public class InvalidResourceException extends RuntimeException
{
	private final String resource;

	public InvalidResourceException(final String message, final String resource)
	{
		super(message);
		this.resource = resource;
	}

	public String getResource()
	{
		return resource;
	}
}
