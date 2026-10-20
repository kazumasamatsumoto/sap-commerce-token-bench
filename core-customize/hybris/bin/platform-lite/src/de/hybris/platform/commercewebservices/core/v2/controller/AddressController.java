package de.hybris.platform.commercewebservices.core.v2.controller;

import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercewebservices.core.exceptions.RequestParameterException;
import de.hybris.platform.commercewebservicescommons.dto.user.AddressListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.user.AddressWsDTO;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/{baseSiteId}/users/{userId}/addresses")
@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT" })
public class AddressController extends BaseController
{
	@Resource(name = "userFacade")
	private UserFacade userFacade;

	/** GET .../addresses */
	@GetMapping
	public AddressListWsDTO getAddresses(@RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final AddressListWsDTO list = new AddressListWsDTO();
		list.setAddresses(userFacade.getAddressBook().stream().map(a -> getDataMapper().map(a, AddressWsDTO.class, fields)).toList());
		return list;
	}

	/** POST .../addresses */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AddressWsDTO createAddress(@RequestBody final AddressWsDTO address, @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		validate(address);
		return getDataMapper().map(userFacade.addAddress(getDataMapper().mapToData(address, AddressData.class)), AddressWsDTO.class, fields);
	}

	/** GET .../addresses/{addressId} */
	@GetMapping("/{addressId}")
	public AddressWsDTO getAddress(@PathVariable final String addressId, @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		return getDataMapper().map(userFacade.getAddressForCode(addressId), AddressWsDTO.class, fields);
	}

	/** PATCH .../addresses/{addressId}(送られた項目だけを変える) */
	@PatchMapping("/{addressId}")
	@ResponseStatus(HttpStatus.OK)
	public void updateAddress(@PathVariable final String addressId, @RequestBody final AddressWsDTO address)
	{
		final AddressData current = userFacade.getAddressForCode(addressId);
		final AddressData patch = getDataMapper().mapToData(address, AddressData.class);
		if (patch.getFirstName() != null) current.setFirstName(patch.getFirstName());
		if (patch.getLastName() != null) current.setLastName(patch.getLastName());
		if (patch.getLine1() != null) current.setLine1(patch.getLine1());
		if (patch.getLine2() != null) current.setLine2(patch.getLine2());
		if (patch.getTown() != null) current.setTown(patch.getTown());
		if (patch.getPostalCode() != null) current.setPostalCode(patch.getPostalCode());
		if (patch.getPhone() != null) current.setPhone(patch.getPhone());
		if (patch.getCountry() != null) current.setCountry(patch.getCountry());
		userFacade.editAddress(addressId, current);
	}

	/** DELETE .../addresses/{addressId} */
	@DeleteMapping("/{addressId}")
	@ResponseStatus(HttpStatus.OK)
	public void deleteAddress(@PathVariable final String addressId)
	{
		userFacade.removeAddress(addressId);
	}

	static void validate(final AddressWsDTO a)
	{
		if (a.getFirstName() == null || a.getFirstName().isBlank())
		{
			throw new RequestParameterException("firstName is required", "firstName");
		}
		if (a.getLastName() == null || a.getLastName().isBlank())
		{
			throw new RequestParameterException("lastName is required", "lastName");
		}
		if (a.getLine1() == null || a.getLine1().isBlank())
		{
			throw new RequestParameterException("line1 is required", "line1");
		}
		if (a.getTown() == null || a.getTown().isBlank())
		{
			throw new RequestParameterException("town is required", "town");
		}
		if (a.getPostalCode() == null || !a.getPostalCode().matches("\\d{3}-?\\d{4}"))
		{
			throw new RequestParameterException("postalCode must look like 123-4567", "postalCode");
		}
		if (a.getCountry() == null || a.getCountry().getIsocode() == null)
		{
			throw new RequestParameterException("country.isocode is required", "country.isocode");
		}
	}
}
