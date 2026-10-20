package de.hybris.platform.servicelayer.interceptor;

/** 保存の前に値を整える(既定値を入れる、計算する)。 */
public interface PrepareInterceptor<MODEL> extends Interceptor
{
	void onPrepare(MODEL model, InterceptorContext ctx) throws InterceptorException;
}
