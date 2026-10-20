package de.hybris.platform.product;

import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.model.product.ProductModel;

public interface ProductService
{
	/** セッションのカタログ(今のサイトの Online)から探す。無ければ UnknownIdentifierException。 */
	ProductModel getProductForCode(String code);

	ProductModel getProductForCode(CatalogVersionModel catalogVersion, String code);
}
