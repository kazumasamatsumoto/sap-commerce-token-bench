package de.hybris.platform.order;

/** 注文にできないカート(空、届け先や配送方法や支払いが無い)。OCC では 400 の InvalidCartError になる。 */
public class InvalidCartException extends Exception
{
	public InvalidCartException(final String message)
	{
		super(message);
	}
}
