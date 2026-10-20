package de.hybris.platform.site;

import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import java.util.List;

public interface BaseSiteService
{
	BaseSiteModel getBaseSiteForUID(String siteUid);

	List<BaseSiteModel> getAllBaseSites();

	BaseSiteModel getCurrentBaseSite();

	void setCurrentBaseSite(BaseSiteModel site, boolean activateCatalogs);
}
