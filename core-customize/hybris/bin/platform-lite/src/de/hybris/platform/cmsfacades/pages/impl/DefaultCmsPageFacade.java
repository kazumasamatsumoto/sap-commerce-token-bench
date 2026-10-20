package de.hybris.platform.cmsfacades.pages.impl;

import de.hybris.bootstrap.typesystem.TypeSystem;
import de.hybris.bootstrap.typesystem.TypeSystem.AttributeDef;
import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cms2.model.contents.components.AbstractCMSComponentModel;
import de.hybris.platform.cms2.model.contents.contentslot.ContentSlotModel;
import de.hybris.platform.cms2.model.pages.AbstractPageModel;
import de.hybris.platform.cms2.model.pages.ContentPageModel;
import de.hybris.platform.cms2.servicelayer.services.CMSPageService;
import de.hybris.platform.cmsfacades.data.ComponentData;
import de.hybris.platform.cmsfacades.data.ContentSlotData;
import de.hybris.platform.cmsfacades.data.PageData;
import de.hybris.platform.cmsfacades.pages.CmsPageFacade;
import de.hybris.platform.core.HybrisEnumValue;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.media.MediaModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultCmsPageFacade implements CmsPageFacade
{
	/** otherProperties に入れない属性(どのコンポーネントにもある、または中の仕組みのもの)。 */
	private static final Set<String> SKIP = Set.of("uid", "name", "catalogversion", "slots", "visible");

	private CMSPageService cmsPageService;
	private FlexibleSearchService flexibleSearchService;
	private CatalogVersionService catalogVersionService;
	private ModelService modelService;
	private TypeSystem typeSystem;

	@Override
	public PageData getPage(final String pageType, final String pageLabelOrId, final String code) throws CMSItemNotFoundException
	{
		final AbstractPageModel page;
		if ("ProductPage".equals(pageType) || "CategoryPage".equals(pageType) || "CatalogPage".equals(pageType))
		{
			page = cmsPageService.getDefaultPage("CatalogPage".equals(pageType) ? "CategoryPage" : pageType);
		}
		else if (pageLabelOrId == null || pageLabelOrId.isBlank() || "homepage".equals(pageLabelOrId) || "/".equals(pageLabelOrId))
		{
			page = cmsPageService.getHomepage();
		}
		else
		{
			page = cmsPageService.getPageForLabelOrId(pageLabelOrId);
		}
		final PageData data = new PageData();
		data.setUid(page.getUid());
		data.setUuid(page.getPk().toString());
		data.setName(page.getName());
		data.setTitle(page.getTitle());
		data.setTypeCode(page.getItemtype());
		data.setTemplate(page.getMasterTemplate() == null ? null : page.getMasterTemplate().getUid());
		data.setLabel(page instanceof ContentPageModel cp ? cp.getLabel() : null);
		final List<ContentSlotData> slots = new ArrayList<>();
		for (final Map.Entry<String, ContentSlotModel> e : cmsPageService.getContentSlotsForPage(page).entrySet())
		{
			final ContentSlotData slot = new ContentSlotData();
			slot.setSlotId(e.getValue().getUid());
			slot.setName(e.getValue().getName());
			slot.setPosition(e.getKey());
			slot.setComponents(e.getValue().getCmsComponents().stream().filter(c -> !Boolean.FALSE.equals(c.getVisible())).map(this::toData).toList());
			slots.add(slot);
		}
		data.setContentSlots(slots);
		return data;
	}

	@Override
	public ComponentData getComponent(final String uid) throws CMSItemNotFoundException
	{
		final List<AbstractCMSComponentModel> list = flexibleSearchService.<AbstractCMSComponentModel> search(
				"SELECT {pk} FROM {AbstractCMSComponent} WHERE {uid} = ?uid AND {catalogVersion} IN (?versions)",
				Map.of("uid", uid, "versions", catalogVersionService.getSessionCatalogVersions())).getResult();
		if (list.isEmpty())
		{
			throw new CMSItemNotFoundException("Component with uid [" + uid + "] not found.");
		}
		return toData(list.get(0));
	}

	@Override
	public List<ComponentData> getComponents(final List<String> uids)
	{
		return flexibleSearchService.<AbstractCMSComponentModel> search(
				"SELECT {pk} FROM {AbstractCMSComponent} WHERE {uid} IN (?uids) AND {catalogVersion} IN (?versions) ORDER BY {uid}",
				Map.of("uids", uids, "versions", catalogVersionService.getSessionCatalogVersions())).getResult().stream().map(this::toData).toList();
	}

	private ComponentData toData(final AbstractCMSComponentModel c)
	{
		final ComponentData d = new ComponentData();
		d.setUid(c.getUid());
		d.setUuid(c.getPk().toString());
		d.setName(c.getName());
		d.setTypeCode(c.getItemtype());
		d.setModifiedtime(c.getModifiedtime());
		final Map<String, Object> other = new LinkedHashMap<>();
		for (final AttributeDef a : typeSystem.allAttributes(typeSystem.getItemType(c.getItemtype())).values())
		{
			if (SKIP.contains(a.qualifier.toLowerCase()) || "Item".equals(a.declaringType))
			{
				continue;
			}
			final Object v = modelService.getAttributeValue(c, a.qualifier);
			if (v != null)
			{
				other.put(a.qualifier, simple(v));
			}
		}
		d.setOtherProperties(other);
		return d;
	}

	/** 本物の OCC と同じく、値は文字列にして返す(メディアは URL、コレクションは空白で区切る)。 */
	private static Object simple(final Object v)
	{
		if (v instanceof MediaModel m)
		{
			return Map.of("url", m.getURL() == null ? "" : m.getURL(), "altText", m.getAltText() == null ? "" : m.getAltText(), "code", m.getCode());
		}
		if (v instanceof ItemModel i)
		{
			return i.getPk().toString();
		}
		if (v instanceof HybrisEnumValue e)
		{
			return e.getCode();
		}
		if (v instanceof Collection<?> col)
		{
			return col.stream().map(o -> String.valueOf(simple(o))).collect(Collectors.joining(" "));
		}
		return String.valueOf(v);
	}

	public void setCmsPageService(final CMSPageService cmsPageService)
	{
		this.cmsPageService = cmsPageService;
	}

	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
	{
		this.flexibleSearchService = flexibleSearchService;
	}

	public void setCatalogVersionService(final CatalogVersionService catalogVersionService)
	{
		this.catalogVersionService = catalogVersionService;
	}

	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

	public void setTypeSystem(final TypeSystem typeSystem)
	{
		this.typeSystem = typeSystem;
	}
}
