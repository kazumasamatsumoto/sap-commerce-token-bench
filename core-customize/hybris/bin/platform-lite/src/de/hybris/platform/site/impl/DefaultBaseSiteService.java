package de.hybris.platform.site.impl;

import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.site.BaseSiteService;
import de.hybris.platform.store.model.BaseStoreModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultBaseSiteService extends AbstractItemDao implements BaseSiteService
{
	public static final String CURRENT_SITE = "currentSite";

	private SessionService sessionService;
	private CatalogVersionService catalogVersionService;

	@Override
	public BaseSiteModel getBaseSiteForUID(final String siteUid)
	{
		final List<BaseSiteModel> list = find("SELECT {pk} FROM {BaseSite} WHERE {uid} = ?uid", Map.of("uid", siteUid));
		return list.isEmpty() ? null : list.get(0);
	}

	@Override
	public List<BaseSiteModel> getAllBaseSites()
	{
		return find("SELECT {pk} FROM {BaseSite} ORDER BY {uid}", Map.of());
	}

	@Override
	public BaseSiteModel getCurrentBaseSite()
	{
		return sessionService.getAttribute(CURRENT_SITE);
	}

	@Override
	public void setCurrentBaseSite(final BaseSiteModel site, final boolean activateCatalogs)
	{
		sessionService.setAttribute(CURRENT_SITE, site);
		if (activateCatalogs && site != null)
		{
			final List<CatalogVersionModel> versions = new ArrayList<>();
			for (final BaseStoreModel store : site.getStores())
			{
				if (store.getProductCatalog() != null)
				{
					versions.add(catalogVersionService.getCatalogVersion(store.getProductCatalog().getId(), "Online"));
				}
			}
			if (site.getContentCatalog() != null)
			{
				versions.add(catalogVersionService.getCatalogVersion(site.getContentCatalog().getId(), "Online"));
			}
			catalogVersionService.setSessionCatalogVersions(versions);
		}
	}

	public void setSessionService(final SessionService sessionService)
	{
		this.sessionService = sessionService;
	}

	public void setCatalogVersionService(final CatalogVersionService catalogVersionService)
	{
		this.catalogVersionService = catalogVersionService;
	}
}
