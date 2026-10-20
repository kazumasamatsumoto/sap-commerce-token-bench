package de.hybris.platform.commercefacades.product.converters.populators;

import de.hybris.platform.basecommerce.enums.StockLevelStatus;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.product.data.StockData;
import de.hybris.platform.commerceservices.stock.CommerceStockService;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.store.model.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

public class ProductStockPopulator implements Populator<ProductModel, ProductData>
{
	private CommerceStockService commerceStockService;
	private BaseStoreService baseStoreService;

	@Override
	public void populate(final ProductModel source, final ProductData target)
	{
		final BaseStoreModel store = baseStoreService.getCurrentBaseStore();
		if (store == null)
		{
			return;
		}
		final StockData stock = new StockData();
		final StockLevelStatus status = commerceStockService.getStockLevelStatusForProductAndBaseStore(source, store);
		stock.setStockLevelStatus(status.getCode());
		stock.setStockLevel(commerceStockService.getStockLevelForProductAndBaseStore(source, store));
		target.setStock(stock);
	}

	public void setCommerceStockService(final CommerceStockService commerceStockService)
	{
		this.commerceStockService = commerceStockService;
	}

	public void setBaseStoreService(final BaseStoreService baseStoreService)
	{
		this.baseStoreService = baseStoreService;
	}
}
