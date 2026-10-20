package de.hybris.platform.commercewebservices.core.v2.controller;

import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.cmsfacades.data.ComponentData;
import de.hybris.platform.cmsfacades.data.ContentSlotData;
import de.hybris.platform.cmsfacades.data.PageData;
import de.hybris.platform.cmsfacades.pages.CmsPageFacade;
import de.hybris.platform.cmsoccaddon.data.CMSPageWsDTO;
import de.hybris.platform.cmsoccaddon.data.ComponentListWsDTO;
import de.hybris.platform.cmsoccaddon.data.ComponentWsDTO;
import de.hybris.platform.cmsoccaddon.data.ContentSlotListWsDTO;
import de.hybris.platform.cmsoccaddon.data.ContentSlotWsDTO;
import de.hybris.platform.cmsoccaddon.data.ListComponentWsDTO;
import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 本物の cmsoccaddon の PageController / ComponentController に当たる。 */
@RestController
@RequestMapping("/{baseSiteId}/cms")
public class CmsPagesController extends BaseController
{
	@Resource(name = "cmsPageFacade")
	private CmsPageFacade cmsPageFacade;

	/** GET /{baseSiteId}/cms/pages?pageType=ContentPage&pageLabelOrId=/cart(または ProductPage&code=XYZ-000001) */
	@GetMapping("/pages")
	public CMSPageWsDTO getPage(@RequestParam(defaultValue = "ContentPage") final String pageType,
			@RequestParam(required = false) final String pageLabelOrId, @RequestParam(required = false) final String code)
			throws CMSItemNotFoundException
	{
		return toWsDTO(cmsPageFacade.getPage(pageType, pageLabelOrId, code));
	}

	/** GET /{baseSiteId}/cms/pages/{pageId} */
	@GetMapping("/pages/{pageId}")
	public CMSPageWsDTO getPageById(@PathVariable final String pageId) throws CMSItemNotFoundException
	{
		return toWsDTO(cmsPageFacade.getPage("ContentPage", pageId, null));
	}

	/** GET /{baseSiteId}/cms/components?componentIds=a,b,c */
	@GetMapping("/components")
	public ListComponentWsDTO getComponents(@RequestParam final String componentIds)
	{
		final ListComponentWsDTO list = new ListComponentWsDTO();
		list.setComponent(cmsPageFacade.getComponents(Arrays.stream(componentIds.split(",")).map(String::trim).toList()).stream()
				.map(CmsPagesController::toWsDTO).toList());
		return list;
	}

	/** GET /{baseSiteId}/cms/components/{componentId} */
	@GetMapping("/components/{componentId}")
	public ComponentWsDTO getComponent(@PathVariable final String componentId) throws CMSItemNotFoundException
	{
		return toWsDTO(cmsPageFacade.getComponent(componentId));
	}

	private static CMSPageWsDTO toWsDTO(final PageData page)
	{
		final CMSPageWsDTO dto = new CMSPageWsDTO();
		dto.setUid(page.getUid());
		dto.setUuid(page.getUuid());
		dto.setTitle(page.getTitle());
		dto.setName(page.getName());
		dto.setTemplate(page.getTemplate());
		dto.setTypeCode(page.getTypeCode());
		dto.setLabel(page.getLabel());
		final List<ContentSlotWsDTO> slots = page.getContentSlots().stream().map(CmsPagesController::toWsDTO).toList();
		final ContentSlotListWsDTO slotList = new ContentSlotListWsDTO();
		slotList.setContentSlot(slots);
		dto.setContentSlots(slotList);
		return dto;
	}

	private static ContentSlotWsDTO toWsDTO(final ContentSlotData slot)
	{
		final ContentSlotWsDTO dto = new ContentSlotWsDTO();
		dto.setSlotId(slot.getSlotId());
		dto.setName(slot.getName());
		dto.setPosition(slot.getPosition());
		final ComponentListWsDTO components = new ComponentListWsDTO();
		components.setComponent(slot.getComponents().stream().map(CmsPagesController::toWsDTO).toList());
		dto.setComponents(components);
		return dto;
	}

	private static ComponentWsDTO toWsDTO(final ComponentData c)
	{
		final ComponentWsDTO dto = new ComponentWsDTO();
		dto.setUid(c.getUid());
		dto.setUuid(c.getUuid());
		dto.setName(c.getName());
		dto.setTypeCode(c.getTypeCode());
		dto.setModifiedtime(c.getModifiedtime());
		dto.setOtherProperties(c.getOtherProperties());
		return dto;
	}
}
