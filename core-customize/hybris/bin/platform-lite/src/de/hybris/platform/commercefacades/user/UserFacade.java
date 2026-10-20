package de.hybris.platform.commercefacades.user;

import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercefacades.user.data.TitleData;
import java.util.List;

/** 住所録と敬称。 */
public interface UserFacade
{
	List<AddressData> getAddressBook();

	AddressData getAddressForCode(String code);

	AddressData addAddress(AddressData address);

	AddressData editAddress(String code, AddressData address);

	void removeAddress(String code);

	List<TitleData> getTitles();
}
