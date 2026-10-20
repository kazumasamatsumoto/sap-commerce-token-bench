package de.hybris.platform.servicelayer.interceptor.impl;

import de.hybris.platform.servicelayer.interceptor.Interceptor;

/**
 * インターセプターと型の結び付け。本物と同じく *-spring.xml に書く:
 * <pre>
 * &lt;bean id="xyzProductValidateInterceptorMapping" class="de.hybris.platform.servicelayer.interceptor.impl.InterceptorMapping"&gt;
 *     &lt;property name="interceptor" ref="xyzProductValidateInterceptor"/&gt;
 *     &lt;property name="typeCode" value="Product"/&gt;
 * &lt;/bean&gt;
 * </pre>
 */
public class InterceptorMapping
{
	private Interceptor interceptor;
	private String typeCode;
	private int order = 0;

	public Interceptor getInterceptor()
	{
		return interceptor;
	}

	public void setInterceptor(final Interceptor interceptor)
	{
		this.interceptor = interceptor;
	}

	public String getTypeCode()
	{
		return typeCode;
	}

	public void setTypeCode(final String typeCode)
	{
		this.typeCode = typeCode;
	}

	public int getOrder()
	{
		return order;
	}

	public void setOrder(final int order)
	{
		this.order = order;
	}
}
