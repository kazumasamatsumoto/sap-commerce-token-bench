package de.hybris.platform.cms2.exceptions;

/** CMS のページやコンポーネントが無いとき。OCC では 400 の CMSItemNotFoundError になる。 */
public class CMSItemNotFoundException extends Exception
{
	public CMSItemNotFoundException(final String message)
	{
		super(message);
	}
}
