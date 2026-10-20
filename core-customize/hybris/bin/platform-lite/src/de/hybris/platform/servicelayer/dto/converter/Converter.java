package de.hybris.platform.servicelayer.dto.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Model から Data を作る。 */
public interface Converter<SOURCE, TARGET>
{
	TARGET convert(SOURCE source) throws ConversionException;

	TARGET convert(SOURCE source, TARGET prototype) throws ConversionException;

	default List<TARGET> convertAll(final Collection<? extends SOURCE> sources) throws ConversionException
	{
		final List<TARGET> out = new ArrayList<>();
		if (sources != null)
		{
			for (final SOURCE s : sources)
			{
				out.add(convert(s));
			}
		}
		return out;
	}
}
