package de.hybris.platform.servicelayer.i18n;

import java.util.Locale;

/** 今の言語。OCC では ?lang=ja の値が入る。 */
public interface I18NService
{
	Locale getCurrentLocale();

	void setCurrentLocale(Locale locale);
}
