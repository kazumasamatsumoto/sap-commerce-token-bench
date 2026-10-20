package de.hybris.platform.cms2.servicelayer.services.impl;

import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.contents.contentslot.ContentSlotModel;
import de.hybris.platform.cms2.model.pages.AbstractPageModel;
import de.hybris.platform.cms2.model.pages.ContentPageModel;
import de.hybris.platform.cms2.model.relations.ContentSlotForPageModel;
import de.hybris.platform.cms2.model.relations.ContentSlotForTemplateModel;
import de.hybris.platform.cms2.servicelayer.services.CMSPageService;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultCMSPageService extends AbstractItemDao implements CMSPageService
{
	private CatalogVersionService catalogVersionService;

	@Override
	public ContentPageModel getPageForLabelOrId(final String labelOrId) throws CMSItemNotFoundException
	{
		final String label = labelOrId.startsWith("/") || "homepage".equals(labelOrId) || "search".equals(labelOrId) ? labelOrId : "/" + labelOrId;
		final List<ContentPageModel> pages = find("SELECT {pk} FROM {ContentPage} WHERE ({label} = ?label OR {label} = ?raw OR {uid} = ?raw)"
				+ " AND {catalogVersion} IN (?versions)", Map.of("label", label, "raw", labelOrId, "versions", catalogVersionService.getSessionCatalogVersions()));
		if (pages.isEmpty())
		{
			throw new CMSItemNotFoundException("No page with label or id [" + labelOrId + "] defined.");
		}
		return pages.get(0);
	}

	@Override
	public ContentPageModel getHomepage() throws CMSItemNotFoundException
	{
		final List<ContentPageModel> pages = find("SELECT {pk} FROM {ContentPage} WHERE {homepage} = true AND {catalogVersion} IN (?versions)",
				Map.of("versions", catalogVersionService.getSessionCatalogVersions()));
		if (pages.isEmpty())
		{
			throw new CMSItemNotFoundException("No homepage defined.");
		}
		return pages.get(0);
	}

	@Override
	public AbstractPageModel getDefaultPage(final String pageTypeCode) throws CMSItemNotFoundException
	{
		final List<AbstractPageModel> pages = find("SELECT {pk} FROM {" + pageTypeCode + "} WHERE {defaultPage} = true AND {catalogVersion} IN (?versions)",
				Map.of("versions", catalogVersionService.getSessionCatalogVersions()));
		if (pages.isEmpty())
		{
			throw new CMSItemNotFoundException("No default page of type [" + pageTypeCode + "] defined.");
		}
		return pages.get(0);
	}

	@Override
	public Map<String, ContentSlotModel> getContentSlotsForPage(final AbstractPageModel page)
	{
		final Map<String, ContentSlotModel> slots = new LinkedHashMap<>();
		final List<ContentSlotForTemplateModel> forTemplate = find("SELECT {pk} FROM {ContentSlotForTemplate} WHERE {pageTemplate} = ?t ORDER BY {pk}",
				Map.of("t", page.getMasterTemplate()));
		for (final ContentSlotForTemplateModel r : forTemplate)
		{
			slots.put(r.getPosition(), r.getContentSlot());
		}
		final List<ContentSlotForPageModel> forPage = find("SELECT {pk} FROM {ContentSlotForPage} WHERE {page} = ?p ORDER BY {pk}", Map.of("p", page));
		for (final ContentSlotForPageModel r : forPage)
		{
			slots.put(r.getPosition(), r.getContentSlot());
		}
		return slots;
	}

	public void setCatalogVersionService(final CatalogVersionService catalogVersionService)
	{
		this.catalogVersionService = catalogVersionService;
	}
}
