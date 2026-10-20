package de.hybris.platform.servicelayer.i18n.impl;

import de.hybris.platform.servicelayer.i18n.I18NService;
import java.util.Locale;

/** 今の言語を、要求(スレッド)ごとに持つ。決めていなければ default.session.language.isocode。 */
public class DefaultI18NService implements I18NService
{
	private final ThreadLocal<Locale> current = new ThreadLocal<>();
	private final Locale defaultLocale;

	public DefaultI18NService(final String defaultLanguage)
	{
		this.defaultLocale = Locale.forLanguageTag(defaultLanguage);
	}

	@Override
	public Locale getCurrentLocale()
	{
		final Locale l = current.get();
		return l != null ? l : defaultLocale;
	}

	@Override
	public void setCurrentLocale(final Locale locale)
	{
		if (locale == null)
		{
			current.remove();
		}
		else
		{
			current.set(locale);
		}
	}
}
