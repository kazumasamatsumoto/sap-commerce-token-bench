package de.hybris.platform.commercefacades.user.converters.populators;

import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.TitleModel;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import java.util.List;
import java.util.Map;

/** 受け取った AddressData を AddressModel に書く(住所の登録と変更)。 */
public class AddressReversePopulator implements Populator<AddressData, AddressModel>
{
	private CommonI18NService commonI18NService;
	private FlexibleSearchService flexibleSearchService;

	@Override
	public void populate(final AddressData source, final AddressModel target)
	{
		if (source.getTitleCode() != null)
		{
			final List<TitleModel> titles = flexibleSearchService.<TitleModel> search("SELECT {pk} FROM {Title} WHERE {code} = ?c",
					Map.of("c", source.getTitleCode())).getResult();
			if (titles.isEmpty())
			{
				throw new ConversionException("No title with the code " + source.getTitleCode() + " found.");
			}
			target.setTitle(titles.get(0));
		}
		target.setFirstname(source.getFirstName());
		target.setLastname(source.getLastName());
		target.setLine1(source.getLine1());
		target.setLine2(source.getLine2());
		target.setTown(source.getTown());
		target.setPostalcode(source.getPostalCode());
		target.setPhone1(source.getPhone());
		target.setEmail(source.getEmail());
		target.setShippingAddress(Boolean.valueOf(source.isShippingAddress() || !source.isBillingAddress()));
		target.setBillingAddress(Boolean.valueOf(source.isBillingAddress()));
		if (source.getCountry() != null && source.getCountry().getIsocode() != null)
		{
			target.setCountry(commonI18NService.getCountry(source.getCountry().getIsocode()));
		}
	}

	public void setCommonI18NService(final CommonI18NService commonI18NService)
	{
		this.commonI18NService = commonI18NService;
	}

	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
	{
		this.flexibleSearchService = flexibleSearchService;
	}
}
