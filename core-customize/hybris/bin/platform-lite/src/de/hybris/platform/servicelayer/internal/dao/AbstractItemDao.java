package de.hybris.platform.servicelayer.internal.dao;

import de.hybris.platform.servicelayer.exceptions.AmbiguousIdentifierException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import java.util.List;
import java.util.Map;

/** DAO の土台。FlexibleSearch を投げる道具を持つ。 */
public abstract class AbstractItemDao
{
	private FlexibleSearchService flexibleSearchService;
	private ModelService modelService;

	protected <T> List<T> find(final String query, final Map<String, ?> params)
	{
		return flexibleSearchService.<T> search(query, params).getResult();
	}

	/** ちょうど 1 件。0 件は UnknownIdentifierException、2 件以上は AmbiguousIdentifierException。 */
	protected <T> T findUnique(final String query, final Map<String, ?> params, final String what)
	{
		final List<T> result = find(query, params);
		if (result.isEmpty())
		{
			throw new UnknownIdentifierException(what + " not found");
		}
		if (result.size() > 1)
		{
			throw new AmbiguousIdentifierException(what + " is not unique, " + result.size() + " items found");
		}
		return result.get(0);
	}

	public FlexibleSearchService getFlexibleSearchService()
	{
		return flexibleSearchService;
	}

	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
	{
		this.flexibleSearchService = flexibleSearchService;
	}

	public ModelService getModelService()
	{
		return modelService;
	}

	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}
}
