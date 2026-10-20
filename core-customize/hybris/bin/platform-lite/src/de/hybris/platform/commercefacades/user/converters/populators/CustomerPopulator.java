package de.hybris.platform.commercefacades.user.converters.populators;

import de.hybris.platform.b2bcommercefacades.company.data.B2BUnitData;
import de.hybris.platform.commercefacades.storesession.data.CurrencyData;
import de.hybris.platform.commercefacades.storesession.data.LanguageData;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.CustomerData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;

/** 名前は「姓 名」の 1 つの値で持っているので、最初の空白で分ける。 */
public class CustomerPopulator implements Populator<CustomerModel, CustomerData>
{
	private Converter<AddressModel, AddressData> addressConverter;

	@Override
	public void populate(final CustomerModel source, final CustomerData target)
	{
		target.setUid(source.getUid());
		target.setName(source.getName());
		target.setDisplayUid(source.getUid());
		target.setCustomerId(source.getCustomerID());
		target.setTitleCode(source.getTitle() == null ? null : source.getTitle().getCode());
		final String name = source.getName() == null ? "" : source.getName().trim();
		final int space = name.indexOf(' ');
		target.setLastName(space < 0 ? name : name.substring(0, space));
		target.setFirstName(space < 0 ? "" : name.substring(space + 1));
		if (source.getSessionLanguage() != null)
		{
			final LanguageData l = new LanguageData();
			l.setIsocode(source.getSessionLanguage().getIsocode());
			l.setName(source.getSessionLanguage().getName());
			target.setLanguage(l);
		}
		if (source.getSessionCurrency() != null)
		{
			final CurrencyData c = new CurrencyData();
			c.setIsocode(source.getSessionCurrency().getIsocode());
			c.setSymbol(source.getSessionCurrency().getSymbol());
			target.setCurrency(c);
		}
		if (source.getDefaultShipmentAddress() != null)
		{
			target.setDefaultShippingAddress(addressConverter.convert(source.getDefaultShipmentAddress()));
		}
		if (source.getDefaultB2BUnit() != null)
		{
			final B2BUnitData unit = new B2BUnitData();
			unit.setUid(source.getDefaultB2BUnit().getUid());
			unit.setName(source.getDefaultB2BUnit().getName());
			unit.setActive(!Boolean.FALSE.equals(source.getDefaultB2BUnit().getActive()));
			target.setOrgUnit(unit);
		}
	}

	public void setAddressConverter(final Converter<AddressModel, AddressData> addressConverter)
	{
		this.addressConverter = addressConverter;
	}
}
