package de.hybris.platform.product.impl;

import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.product.ProductService;
import de.hybris.platform.servicelayer.exceptions.AmbiguousIdentifierException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import java.util.List;
import java.util.Map;

public class DefaultProductService extends AbstractItemDao implements ProductService
{
	private CatalogVersionService catalogVersionService;

	@Override
	public ProductModel getProductForCode(final String code)
	{
		final List<ProductModel> list = find("SELECT {pk} FROM {Product} WHERE {code} = ?code AND {catalogVersion} IN (?versions)",
				Map.of("code", code, "versions", catalogVersionService.getSessionCatalogVersions()));
		if (list.isEmpty())
		{
			throw new UnknownIdentifierException("Product with code '" + code + "' not found!");
		}
		if (list.size() > 1)
		{
			throw new AmbiguousIdentifierException("Product code '" + code + "' is not unique, " + list.size() + " products found!");
		}
		return list.get(0);
	}

	@Override
	public ProductModel getProductForCode(final CatalogVersionModel catalogVersion, final String code)
	{
		return findUnique("SELECT {pk} FROM {Product} WHERE {code} = ?code AND {catalogVersion} = ?cv", Map.of("code", code, "cv", catalogVersion),
				"Product with code '" + code + "'");
	}

	public void setCatalogVersionService(final CatalogVersionService catalogVersionService)
	{
		this.catalogVersionService = catalogVersionService;
	}
}
