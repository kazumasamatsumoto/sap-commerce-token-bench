package de.hybris.platform.b2b.services.impl;

import de.hybris.platform.b2b.model.B2BUnitModel;
import de.hybris.platform.b2b.services.B2BUnitService;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import java.util.List;
import java.util.Map;

public class DefaultB2BUnitService extends AbstractItemDao implements B2BUnitService
{
	@Override
	public B2BUnitModel getUnitForUid(final String uid)
	{
		return findUnique("SELECT {pk} FROM {B2BUnit} WHERE {uid} = ?uid", Map.of("uid", uid), "B2BUnit with uid '" + uid + "'");
	}

	@Override
	public List<B2BUnitModel> getAllUnits()
	{
		return find("SELECT {pk} FROM {B2BUnit} ORDER BY {uid}", Map.of());
	}

	@Override
	public B2BUnitModel getParent(final CustomerModel customer)
	{
		return customer.getDefaultB2BUnit();
	}
}
