package de.hybris.platform.servicelayer.i18n.impl;

import de.hybris.platform.core.model.c2l.CountryModel;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.i18n.I18NService;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import de.hybris.platform.servicelayer.session.SessionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DefaultCommonI18NService extends AbstractItemDao implements CommonI18NService
{
	private static final String CURRENT_CURRENCY = "currency";

	private SessionService sessionService;
	private I18NService i18NService;
	private String defaultCurrencyIsocode = "JPY";

	@Override
	public LanguageModel getLanguage(final String isocode)
	{
		return byIsocode("Language", isocode);
	}

	@Override
	public CurrencyModel getCurrency(final String isocode)
	{
		return byIsocode("Currency", isocode);
	}

	@Override
	public CountryModel getCountry(final String isocode)
	{
		return byIsocode("Country", isocode);
	}

	private <T> T byIsocode(final String type, final String isocode)
	{
		final List<T> list = find("SELECT {pk} FROM {" + type + "} WHERE {isocode} = ?iso", Map.of("iso", isocode));
		if (list.isEmpty())
		{
			throw new UnknownIdentifierException(type + " with isocode '" + isocode + "' not found!");
		}
		return list.get(0);
	}

	@Override
	public List<LanguageModel> getAllLanguages()
	{
		return find("SELECT {pk} FROM {Language} WHERE {active} = true ORDER BY {isocode}", Map.of());
	}

	@Override
	public List<CurrencyModel> getAllCurrencies()
	{
		return find("SELECT {pk} FROM {Currency} WHERE {active} = true ORDER BY {isocode}", Map.of());
	}

	@Override
	public List<CountryModel> getAllCountries()
	{
		return find("SELECT {pk} FROM {Country} WHERE {active} = true ORDER BY {isocode}", Map.of());
	}

	@Override
	public LanguageModel getCurrentLanguage()
	{
		return getLanguage(i18NService.getCurrentLocale().getLanguage());
	}

	@Override
	public void setCurrentLanguage(final LanguageModel language)
	{
		i18NService.setCurrentLocale(Locale.forLanguageTag(language.getIsocode()));
	}

	@Override
	public CurrencyModel getCurrentCurrency()
	{
		final CurrencyModel c = sessionService.getAttribute(CURRENT_CURRENCY);
		return c != null ? c : getCurrency(defaultCurrencyIsocode);
	}

	@Override
	public void setCurrentCurrency(final CurrencyModel currency)
	{
		sessionService.setAttribute(CURRENT_CURRENCY, currency);
	}

	@Override
	public double roundCurrency(final double value, final int digits)
	{
		return BigDecimal.valueOf(value).setScale(digits, RoundingMode.HALF_UP).doubleValue();
	}

	public void setSessionService(final SessionService sessionService)
	{
		this.sessionService = sessionService;
	}

	public void setI18NService(final I18NService i18NService)
	{
		this.i18NService = i18NService;
	}

	public void setDefaultCurrencyIsocode(final String defaultCurrencyIsocode)
	{
		this.defaultCurrencyIsocode = defaultCurrencyIsocode;
	}
}
