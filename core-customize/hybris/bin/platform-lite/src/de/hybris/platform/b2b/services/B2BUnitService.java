package de.hybris.platform.b2b.services;

import de.hybris.platform.b2b.model.B2BUnitModel;
import de.hybris.platform.core.model.user.CustomerModel;
import java.util.List;

public interface B2BUnitService
{
	B2BUnitModel getUnitForUid(String uid);

	List<B2BUnitModel> getAllUnits();

	B2BUnitModel getParent(CustomerModel customer);
}
