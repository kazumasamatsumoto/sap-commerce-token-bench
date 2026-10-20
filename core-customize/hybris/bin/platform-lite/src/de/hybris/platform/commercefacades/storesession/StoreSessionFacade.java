package de.hybris.platform.commercefacades.storesession;

import de.hybris.platform.commercefacades.storesession.data.CurrencyData;
import de.hybris.platform.commercefacades.storesession.data.LanguageData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import java.util.List;

public interface StoreSessionFacade
{
	List<LanguageData> getAllLanguages();

	List<CurrencyData> getAllCurrencies();

	List<CountryData> getAllCountries();
}
