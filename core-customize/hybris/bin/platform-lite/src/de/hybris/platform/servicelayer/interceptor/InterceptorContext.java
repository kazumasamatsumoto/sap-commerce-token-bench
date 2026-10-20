package de.hybris.platform.servicelayer.interceptor;

import de.hybris.platform.servicelayer.model.ModelService;

/** インターセプターに渡される、保存中の状態。 */
public interface InterceptorContext
{
	ModelService getModelService();

	boolean isNew(Object model);

	boolean isModified(Object model, String attribute);
}
