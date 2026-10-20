package de.hybris.platform.servicelayer.interceptor;

/** インターセプターが保存を止めるときに投げる。ModelService は ModelSavingException に包んで返す。 */
public class InterceptorException extends Exception
{
	public InterceptorException(final String message)
	{
		super(message);
	}

	public InterceptorException(final String message, final Throwable cause)
	{
		super(message, cause);
	}
}
