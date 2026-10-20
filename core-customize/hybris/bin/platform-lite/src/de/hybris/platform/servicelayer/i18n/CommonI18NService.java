package de.hybris.platform.servicelayer.i18n;

import de.hybris.platform.core.model.c2l.CountryModel;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import java.util.List;

public interface CommonI18NService
{
	LanguageModel getLanguage(String isocode);

	CurrencyModel getCurrency(String isocode);

	CountryModel getCountry(String isocode);

	List<LanguageModel> getAllLanguages();

	List<CurrencyModel> getAllCurrencies();

	List<CountryModel> getAllCountries();

	LanguageModel getCurrentLanguage();

	void setCurrentLanguage(LanguageModel language);

	CurrencyModel getCurrentCurrency();

	void setCurrentCurrency(CurrencyModel currency);

	/** 通貨の桁(digits)で丸める。 */
	double roundCurrency(double value, int digits);
}
