package de.hybris.platform.commerceservices.price;

import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.europe1.model.PriceRowModel;

public interface CommercePriceService
{
	/** 今の通貨の、画面に出す価格。無ければ null(商品は買えない)。 */
	PriceRowModel getWebPriceForProduct(ProductModel product);
}
