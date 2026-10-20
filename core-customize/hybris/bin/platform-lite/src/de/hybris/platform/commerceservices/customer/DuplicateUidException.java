package de.hybris.platform.commerceservices.customer;

/** 登録しようとした uid(メールアドレス)が使われているとき。OCC では 400 の DuplicateUidError になる。 */
public class DuplicateUidException extends Exception
{
	public DuplicateUidException(final String message)
	{
		super(message);
	}
}
