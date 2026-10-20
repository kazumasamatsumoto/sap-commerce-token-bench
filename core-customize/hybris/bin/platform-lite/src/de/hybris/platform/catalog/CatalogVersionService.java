package de.hybris.platform.catalog;

import de.hybris.platform.catalog.model.CatalogVersionModel;
import java.util.Collection;

public interface CatalogVersionService
{
	CatalogVersionModel getCatalogVersion(String catalogId, String version);

	Collection<CatalogVersionModel> getSessionCatalogVersions();

	void setSessionCatalogVersions(Collection<CatalogVersionModel> catalogVersions);

	void setSessionCatalogVersion(String catalogId, String version);
}
