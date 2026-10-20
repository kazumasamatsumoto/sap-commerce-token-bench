package de.hybris.platform.order;

import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.delivery.DeliveryModeModel;
import java.util.List;

public interface DeliveryModeService
{
	DeliveryModeModel getDeliveryModeForCode(String code);

	/** その注文(カート)の店で使える配送方法。 */
	List<DeliveryModeModel> getSupportedDeliveryModes(AbstractOrderModel order);
}
