package de.hybris.platform.commerceservices.stock;

import de.hybris.platform.basecommerce.enums.StockLevelStatus;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.store.model.BaseStoreModel;

public interface CommerceStockService
{
	/** 在庫の数。forceInStock なら null(数えない = いくらでもある)。 */
	Long getStockLevelForProductAndBaseStore(ProductModel product, BaseStoreModel baseStore);

	StockLevelStatus getStockLevelStatusForProductAndBaseStore(ProductModel product, BaseStoreModel baseStore);
}
