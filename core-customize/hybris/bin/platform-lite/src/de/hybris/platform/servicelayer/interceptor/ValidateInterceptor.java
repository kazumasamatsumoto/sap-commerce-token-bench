package de.hybris.platform.servicelayer.interceptor;

/** 保存の前に値を確かめる。だめなら InterceptorException を投げて保存を止める。 */
public interface ValidateInterceptor<MODEL> extends Interceptor
{
	void onValidate(MODEL model, InterceptorContext ctx) throws InterceptorException;
}
