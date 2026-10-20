package de.hybris.platform.cmsfacades.pages;

import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cmsfacades.data.ComponentData;
import de.hybris.platform.cmsfacades.data.PageData;
import java.util.List;

/** OCC の /cms/pages と /cms/components の中身。 */
public interface CmsPageFacade
{
	/** pageType: ContentPage(pageLabelOrId で探す)、ProductPage / CategoryPage(既定のページ)。 */
	PageData getPage(String pageType, String pageLabelOrId, String code) throws CMSItemNotFoundException;

	ComponentData getComponent(String uid) throws CMSItemNotFoundException;

	List<ComponentData> getComponents(List<String> uids);
}
