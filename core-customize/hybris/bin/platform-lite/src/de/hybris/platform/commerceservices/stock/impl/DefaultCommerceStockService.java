package de.hybris.platform.commerceservices.stock.impl;

import de.hybris.platform.basecommerce.enums.InStockStatus;
import de.hybris.platform.basecommerce.enums.StockLevelStatus;
import de.hybris.platform.commerceservices.stock.CommerceStockService;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.ordersplitting.model.StockLevelModel;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import de.hybris.platform.store.model.BaseStoreModel;
import java.util.List;
import java.util.Map;

public class DefaultCommerceStockService extends AbstractItemDao implements CommerceStockService
{
	private int lowStockThreshold = 5;

	private StockLevelModel stockLevel(final ProductModel product, final BaseStoreModel store)
	{
		final List<StockLevelModel> list = find("SELECT {pk} FROM {StockLevel} WHERE {productCode} = ?code AND {warehouse} = ?wh",
				Map.of("code", product.getCode(), "wh", store.getWarehouse() == null ? "" : store.getWarehouse()));
		return list.isEmpty() ? null : list.get(0);
	}

	@Override
	public Long getStockLevelForProductAndBaseStore(final ProductModel product, final BaseStoreModel baseStore)
	{
		final StockLevelModel s = stockLevel(product, baseStore);
		if (s == null || s.getInStockStatus() == InStockStatus.FORCEOUTOFSTOCK)
		{
			return 0L;
		}
		if (s.getInStockStatus() == InStockStatus.FORCEINSTOCK)
		{
			return null;
		}
		return (long) Math.max(0, s.getAvailable() - s.getReserved());
	}

	@Override
	public StockLevelStatus getStockLevelStatusForProductAndBaseStore(final ProductModel product, final BaseStoreModel baseStore)
	{
		final Long level = getStockLevelForProductAndBaseStore(product, baseStore);
		if (level == null)
		{
			return StockLevelStatus.INSTOCK;
		}
		if (level <= 0)
		{
			return StockLevelStatus.OUTOFSTOCK;
		}
		return level <= lowStockThreshold ? StockLevelStatus.LOWSTOCK : StockLevelStatus.INSTOCK;
	}

	public void setLowStockThreshold(final int lowStockThreshold)
	{
		this.lowStockThreshold = lowStockThreshold;
	}
}
