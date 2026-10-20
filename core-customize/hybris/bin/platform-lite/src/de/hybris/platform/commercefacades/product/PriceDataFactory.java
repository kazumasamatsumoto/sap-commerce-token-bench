package de.hybris.platform.commercefacades.product;

import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.commercefacades.product.data.PriceDataType;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/** 金額を PriceData(値と、通貨の書き方で整えた文字列)にする。 */
public class PriceDataFactory
{
	public PriceData create(final PriceDataType type, final BigDecimal value, final CurrencyModel currency)
	{
		final PriceData p = new PriceData();
		final int digits = currency.getDigits() == null ? 2 : currency.getDigits();
		final BigDecimal scaled = value.setScale(digits, RoundingMode.HALF_UP);
		p.setPriceType(type);
		p.setValue(scaled);
		p.setCurrencyIso(currency.getIsocode());
		final NumberFormat nf = NumberFormat.getNumberInstance(Locale.JAPAN);
		nf.setMinimumFractionDigits(digits);
		nf.setMaximumFractionDigits(digits);
		p.setFormattedValue((currency.getSymbol() == null ? currency.getIsocode() + " " : currency.getSymbol()) + nf.format(scaled));
		return p;
	}

	public PriceData create(final PriceDataType type, final double value, final CurrencyModel currency)
	{
		return create(type, BigDecimal.valueOf(value), currency);
	}
}
