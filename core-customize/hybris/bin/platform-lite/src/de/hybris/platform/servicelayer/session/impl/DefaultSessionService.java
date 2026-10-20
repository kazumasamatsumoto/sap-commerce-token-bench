package de.hybris.platform.servicelayer.session.impl;

import de.hybris.platform.servicelayer.session.SessionService;
import java.util.HashMap;
import java.util.Map;

public class DefaultSessionService implements SessionService
{
	private final ThreadLocal<Map<String, Object>> attributes = ThreadLocal.withInitial(HashMap::new);

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAttribute(final String name)
	{
		return (T) attributes.get().get(name);
	}

	@Override
	public void setAttribute(final String name, final Object value)
	{
		attributes.get().put(name, value);
	}

	@Override
	public void removeAttribute(final String name)
	{
		attributes.get().remove(name);
	}

	@Override
	public void closeCurrentSession()
	{
		attributes.remove();
	}
}
