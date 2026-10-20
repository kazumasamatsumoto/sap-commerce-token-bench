package de.hybris.platform.commercewebservices.core.v2.controller;

import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.commercewebservicescommons.dto.basesite.BaseSiteListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.basesite.BaseSiteWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.storesession.LanguageWsDTO;
import de.hybris.platform.site.BaseSiteService;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basesites")
public class BaseSitesController extends BaseController
{
	@Resource(name = "baseSiteService")
	private BaseSiteService baseSiteService;

	/** GET /basesites … サイトの一覧。 */
	@GetMapping
	public BaseSiteListWsDTO getBaseSites()
	{
		final List<BaseSiteWsDTO> sites = new ArrayList<>();
		for (final BaseSiteModel s : baseSiteService.getAllBaseSites())
		{
			final BaseSiteWsDTO d = new BaseSiteWsDTO();
			d.setUid(s.getUid());
			d.setName(s.getName());
			d.setChannel(s.getChannel() == null ? null : s.getChannel().getCode());
			d.setStores(s.getStores().stream().map(st -> st.getUid()).toList());
			if (s.getDefaultLanguage() != null)
			{
				final LanguageWsDTO l = new LanguageWsDTO();
				l.setIsocode(s.getDefaultLanguage().getIsocode());
				d.setDefaultLanguage(l);
			}
			sites.add(d);
		}
		final BaseSiteListWsDTO list = new BaseSiteListWsDTO();
		list.setBaseSites(sites);
		return list;
	}
}
