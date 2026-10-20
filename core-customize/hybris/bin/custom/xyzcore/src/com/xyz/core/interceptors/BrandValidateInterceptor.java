package com.xyz.core.interceptors;

import com.xyz.core.model.BrandModel;
import de.hybris.platform.servicelayer.interceptor.InterceptorContext;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;
import de.hybris.platform.servicelayer.interceptor.ValidateInterceptor;

/** ブランドの code は英小文字・数字・ハイフンだけ(URL に使うため)。 */
public class BrandValidateInterceptor implements ValidateInterceptor<BrandModel>
{
	private static final String CODE_PATTERN = "[a-z0-9][a-z0-9-]*";

	@Override
	public void onValidate(final BrandModel brand, final InterceptorContext ctx) throws InterceptorException
	{
		if (brand.getCode() != null && !brand.getCode().matches(CODE_PATTERN))
		{
			throw new InterceptorException("Brand code '" + brand.getCode() + "' must match " + CODE_PATTERN);
		}
	}
}
