package de.hybris.platform.commercewebservices.core.v2.controller;

import de.hybris.platform.b2b.model.B2BUnitModel;
import de.hybris.platform.b2b.services.B2BUnitService;
import de.hybris.platform.b2bwebservicescommons.dto.company.B2BUnitListWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.B2BUnitWsDTO;
import jakarta.annotation.Resource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** B2B の組織(本物の b2bocc の OrgUnitsController に当たる。読むだけ)。 */
@RestController
@RequestMapping("/{baseSiteId}/orgUnits")
@Secured({ "ROLE_B2BCUSTOMERGROUP", "ROLE_TRUSTED_CLIENT" })
public class OrgUnitsController extends BaseController
{
	@Resource(name = "b2bUnitService")
	private B2BUnitService b2bUnitService;

	/** GET /{baseSiteId}/orgUnits */
	@GetMapping
	public B2BUnitListWsDTO getUnits()
	{
		final B2BUnitListWsDTO list = new B2BUnitListWsDTO();
		list.setUnits(b2bUnitService.getAllUnits().stream().map(OrgUnitsController::toWsDTO).toList());
		return list;
	}

	/** GET /{baseSiteId}/orgUnits/{orgUnitId} */
	@GetMapping("/{orgUnitId}")
	public B2BUnitWsDTO getUnit(@PathVariable final String orgUnitId)
	{
		return toWsDTO(b2bUnitService.getUnitForUid(orgUnitId));
	}

	private static B2BUnitWsDTO toWsDTO(final B2BUnitModel unit)
	{
		final B2BUnitWsDTO d = new B2BUnitWsDTO();
		d.setUid(unit.getUid());
		d.setName(unit.getLocName() != null ? unit.getLocName() : unit.getName());
		d.setActive(!Boolean.FALSE.equals(unit.getActive()));
		return d;
	}
}
