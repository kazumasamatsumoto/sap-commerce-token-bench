package de.hybris.bootstrap.typesystem;

/** items.xml の書き間違い。ビルド(ant build)で出ると、本物と同じくビルドが止まる。 */
public class TypeSystemException extends RuntimeException
{
	public TypeSystemException(final String message)
	{
		super(message);
	}

	public TypeSystemException(final String message, final Throwable cause)
	{
		super(message, cause);
	}
}
