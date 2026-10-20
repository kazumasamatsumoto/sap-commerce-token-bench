package de.hybris.platform.commercewebservices.core.v2.controller;

import de.hybris.platform.commercefacades.storesession.StoreSessionFacade;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commercewebservicescommons.dto.order.CardTypeWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.storesession.CurrencyListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.storesession.CurrencyWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.storesession.LanguageListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.storesession.LanguageWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.user.CountryListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.user.CountryWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.user.TitleListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.user.TitleWsDTO;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/{baseSiteId}")
public class MiscsController extends BaseController
{
	@Resource(name = "storeSessionFacade")
	private StoreSessionFacade storeSessionFacade;

	@Resource(name = "userFacade")
	private UserFacade userFacade;

	/** GET /{baseSiteId}/languages */
	@GetMapping("/languages")
	public LanguageListWsDTO getLanguages(@RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final LanguageListWsDTO list = new LanguageListWsDTO();
		list.setLanguages(storeSessionFacade.getAllLanguages().stream().map(l -> getDataMapper().map(l, LanguageWsDTO.class, fields)).toList());
		return list;
	}

	/** GET /{baseSiteId}/currencies */
	@GetMapping("/currencies")
	public CurrencyListWsDTO getCurrencies(@RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final CurrencyListWsDTO list = new CurrencyListWsDTO();
		list.setCurrencies(storeSessionFacade.getAllCurrencies().stream().map(c -> getDataMapper().map(c, CurrencyWsDTO.class, fields)).toList());
		return list;
	}

	/** GET /{baseSiteId}/countries?type=SHIPPING|BILLING(どちらも同じ一覧を返す) */
	@GetMapping("/countries")
	public CountryListWsDTO getCountries(@RequestParam(required = false) final String type,
			@RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final CountryListWsDTO list = new CountryListWsDTO();
		list.setCountries(storeSessionFacade.getAllCountries().stream().map(c -> getDataMapper().map(c, CountryWsDTO.class, fields)).toList());
		return list;
	}

	/** GET /{baseSiteId}/titles */
	@GetMapping("/titles")
	public TitleListWsDTO getTitles(@RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final TitleListWsDTO list = new TitleListWsDTO();
		list.setTitles(userFacade.getTitles().stream().map(t -> getDataMapper().map(t, TitleWsDTO.class, fields)).toList());
		return list;
	}

	/** GET /{baseSiteId}/cardtypes */
	@GetMapping("/cardtypes")
	public Map<String, List<CardTypeWsDTO>> getCardTypes()
	{
		return Map.of("cardTypes", List.of(card("visa", "Visa"), card("master", "Mastercard"), card("jcb", "JCB"), card("amex", "American Express")));
	}

	private static CardTypeWsDTO card(final String code, final String name)
	{
		final CardTypeWsDTO c = new CardTypeWsDTO();
		c.setCode(code);
		c.setName(name);
		return c;
	}
}
