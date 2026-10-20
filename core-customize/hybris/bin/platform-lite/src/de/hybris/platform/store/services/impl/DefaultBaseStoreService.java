package de.hybris.platform.store.services.impl;

import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import de.hybris.platform.site.BaseSiteService;
import de.hybris.platform.store.model.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;
import java.util.Map;

/** 今の店 = 今のサイトの最初の店(本物の BaseSiteBaseStoreSelectorStrategy と同じ考え)。 */
public class DefaultBaseStoreService extends AbstractItemDao implements BaseStoreService
{
	private BaseSiteService baseSiteService;

	@Override
	public BaseStoreModel getCurrentBaseStore()
	{
		final BaseSiteModel site = baseSiteService.getCurrentBaseSite();
		if (site == null || site.getStores().isEmpty())
		{
			return null;
		}
		return site.getStores().get(0);
	}

	@Override
	public BaseStoreModel getBaseStoreForUid(final String uid)
	{
		try
		{
			return findUnique("SELECT {pk} FROM {BaseStore} WHERE {uid} = ?uid", Map.of("uid", uid), "BaseStore with uid '" + uid + "'");
		}
		catch (final UnknownIdentifierException e)
		{
			throw new UnknownIdentifierException("BaseStore with uid '" + uid + "' not found!");
		}
	}

	public void setBaseSiteService(final BaseSiteService baseSiteService)
	{
		this.baseSiteService = baseSiteService;
	}
}
