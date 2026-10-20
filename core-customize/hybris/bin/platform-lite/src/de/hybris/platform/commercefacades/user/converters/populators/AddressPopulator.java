package de.hybris.platform.commercefacades.user.converters.populators;

import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.user.AddressModel;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AddressPopulator implements Populator<AddressModel, AddressData>
{
	@Override
	public void populate(final AddressModel source, final AddressData target)
	{
		target.setId(source.getPk() == null ? null : source.getPk().toString());
		target.setTitleCode(source.getTitle() == null ? null : source.getTitle().getCode());
		target.setFirstName(source.getFirstname());
		target.setLastName(source.getLastname());
		target.setLine1(source.getLine1());
		target.setLine2(source.getLine2());
		target.setTown(source.getTown());
		target.setPostalCode(source.getPostalcode());
		target.setPhone(source.getPhone1());
		target.setEmail(source.getEmail());
		target.setShippingAddress(Boolean.TRUE.equals(source.getShippingAddress()));
		target.setBillingAddress(Boolean.TRUE.equals(source.getBillingAddress()));
		target.setVisibleInAddressBook(true);
		if (source.getCountry() != null)
		{
			final CountryData c = new CountryData();
			c.setIsocode(source.getCountry().getIsocode());
			c.setName(source.getCountry().getName());
			target.setCountry(c);
		}
		target.setFormattedAddress(Stream.of(source.getPostalcode(), source.getTown(), source.getLine1(), source.getLine2()).filter(Objects::nonNull)
				.filter(s -> !s.isBlank()).collect(Collectors.joining(" ")));
	}
}
