package de.hybris.platform.converters;

import de.hybris.platform.servicelayer.dto.converter.ConversionException;

/** source の値を target に写す。1 つの Populator は 1 つの関心ごと(価格、画像など)だけを扱う。 */
public interface Populator<SOURCE, TARGET>
{
	void populate(SOURCE source, TARGET target) throws ConversionException;
}
