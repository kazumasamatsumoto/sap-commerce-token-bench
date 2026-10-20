package de.hybris.platform.order.impl;

import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.delivery.DeliveryModeModel;
import de.hybris.platform.order.DeliveryModeService;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DefaultDeliveryModeService extends AbstractItemDao implements DeliveryModeService
{
	@Override
	public DeliveryModeModel getDeliveryModeForCode(final String code)
	{
		return findUnique("SELECT {pk} FROM {DeliveryMode} WHERE {code} = ?code", Map.of("code", code), "DeliveryMode with code '" + code + "'");
	}

	@Override
	public List<DeliveryModeModel> getSupportedDeliveryModes(final AbstractOrderModel order)
	{
		if (order.getStore() == null)
		{
			return List.of();
		}
		return order.getStore().getDeliveryModes().stream().filter(m -> !Boolean.FALSE.equals(m.getActive()))
				.sorted(Comparator.comparing(m -> m.getCost() == null ? 0d : m.getCost())).toList();
	}
}
