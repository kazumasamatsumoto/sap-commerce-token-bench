package de.hybris.platform.commercewebservices.core.exceptions;

/** 受け取った値の誤り。OCC では 400 の ValidationError(または RequestParameterError)。 */
public class RequestParameterException extends RuntimeException
{
	private final String subject;

	public RequestParameterException(final String message, final String subject)
	{
		super(message);
		this.subject = subject;
	}

	public String getSubject()
	{
		return subject;
	}
}
