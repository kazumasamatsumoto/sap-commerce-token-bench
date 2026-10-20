package de.hybris.platform.order;

import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.order.exceptions.CalculationException;

public interface CalculationService
{
	/** 明細の価格、小計、送料、税、合計を計算し直して保存する。 */
	void calculate(AbstractOrderModel order) throws CalculationException;
}
