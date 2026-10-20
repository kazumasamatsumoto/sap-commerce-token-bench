package de.hybris.platform.commerceservices.order;

/** カートを変えられないとき(在庫切れ、数量の誤りなど)。OCC では 400 の CartError になる。 */
public class CommerceCartModificationException extends Exception
{
	public CommerceCartModificationException(final String message)
	{
		super(message);
	}
}
