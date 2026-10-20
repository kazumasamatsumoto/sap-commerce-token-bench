package de.hybris.platform.commercefacades.storesession.impl;

import de.hybris.platform.commercefacades.storesession.StoreSessionFacade;
import de.hybris.platform.commercefacades.storesession.data.CurrencyData;
import de.hybris.platform.commercefacades.storesession.data.LanguageData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.core.model.c2l.CountryModel;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import java.util.List;
import java.util.Locale;

public class DefaultStoreSessionFacade implements StoreSessionFacade
{
	private CommonI18NService commonI18NService;

	@Override
	public List<LanguageData> getAllLanguages()
	{
		return commonI18NService.getAllLanguages().stream().map(this::language).toList();
	}

	private LanguageData language(final LanguageModel l)
	{
		final LanguageData d = new LanguageData();
		d.setIsocode(l.getIsocode());
		d.setName(l.getName());
		d.setNativeName(l.getName(Locale.forLanguageTag(l.getIsocode())));
		d.setActive(!Boolean.FALSE.equals(l.getActive()));
		return d;
	}

	@Override
	public List<CurrencyData> getAllCurrencies()
	{
		return commonI18NService.getAllCurrencies().stream().map(this::currency).toList();
	}

	private CurrencyData currency(final CurrencyModel c)
	{
		final CurrencyData d = new CurrencyData();
		d.setIsocode(c.getIsocode());
		d.setName(c.getName());
		d.setSymbol(c.getSymbol());
		d.setActive(!Boolean.FALSE.equals(c.getActive()));
		return d;
	}

	@Override
	public List<CountryData> getAllCountries()
	{
		return commonI18NService.getAllCountries().stream().map(this::country).toList();
	}

	private CountryData country(final CountryModel c)
	{
		final CountryData d = new CountryData();
		d.setIsocode(c.getIsocode());
		d.setName(c.getName());
		return d;
	}

	public void setCommonI18NService(final CommonI18NService commonI18NService)
	{
		this.commonI18NService = commonI18NService;
	}
}
