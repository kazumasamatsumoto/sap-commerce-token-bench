package de.hybris.platform.core;

import org.springframework.context.ApplicationContext;

/** 本物と同じく、Spring の外(Model の中など)から bean を引くための入口。新しいコードでは注入を使う。 */
public final class Registry
{
	private static ApplicationContext applicationContext;

	private Registry()
	{
	}

	public static ApplicationContext getApplicationContext()
	{
		if (applicationContext == null)
		{
			throw new IllegalStateException("Registry is not started (no application context)");
		}
		return applicationContext;
	}

	public static boolean hasApplicationContext()
	{
		return applicationContext != null;
	}

	public static void setApplicationContext(final ApplicationContext ctx)
	{
		applicationContext = ctx;
	}
}
