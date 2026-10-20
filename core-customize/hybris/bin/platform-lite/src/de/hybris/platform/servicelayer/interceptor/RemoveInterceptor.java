package de.hybris.platform.servicelayer.interceptor;

public interface RemoveInterceptor<MODEL> extends Interceptor
{
	void onRemove(MODEL model, InterceptorContext ctx) throws InterceptorException;
}
