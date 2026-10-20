package de.hybris.platform.catalog.impl;

import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import de.hybris.platform.servicelayer.session.SessionService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DefaultCatalogVersionService extends AbstractItemDao implements CatalogVersionService
{
	public static final String SESSION_CATALOG_VERSIONS = "catalogversions";

	private SessionService sessionService;

	@Override
	public CatalogVersionModel getCatalogVersion(final String catalogId, final String version)
	{
		final List<CatalogVersionModel> list = find(
				"SELECT {cv.pk} FROM {CatalogVersion AS cv JOIN Catalog AS c ON {cv.catalog} = {c.pk}} WHERE {c.id} = ?id AND {cv.version} = ?version",
				Map.of("id", catalogId, "version", version));
		if (list.isEmpty())
		{
			throw new UnknownIdentifierException("CatalogVersion with catalogId '" + catalogId + "' and version '" + version + "' not found!");
		}
		return list.get(0);
	}

	@Override
	public Collection<CatalogVersionModel> getSessionCatalogVersions()
	{
		final Collection<CatalogVersionModel> v = sessionService.getAttribute(SESSION_CATALOG_VERSIONS);
		return v == null ? List.of() : v;
	}

	@Override
	public void setSessionCatalogVersions(final Collection<CatalogVersionModel> catalogVersions)
	{
		sessionService.setAttribute(SESSION_CATALOG_VERSIONS, new ArrayList<>(catalogVersions));
	}

	@Override
	public void setSessionCatalogVersion(final String catalogId, final String version)
	{
		setSessionCatalogVersions(List.of(getCatalogVersion(catalogId, version)));
	}

	public void setSessionService(final SessionService sessionService)
	{
		this.sessionService = sessionService;
	}
}
