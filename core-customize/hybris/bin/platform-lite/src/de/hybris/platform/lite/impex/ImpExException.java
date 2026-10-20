package de.hybris.platform.lite.impex;

/** ImpEx の書き方そのものの誤り(ヘッダーの型や属性が無いなど)。何周しても直らないので、すぐ止める。 */
public class ImpExException extends RuntimeException
{
	public ImpExException(final String message)
	{
		super(message);
	}
}
