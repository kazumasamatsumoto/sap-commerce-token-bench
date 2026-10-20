package de.hybris.platform.commercefacades.product.converters.populators;

import de.hybris.platform.catalog.enums.ArticleApprovalStatus;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.product.ProductModel;

/** code、name、summary、description、manufacturer、ean、purchasable、url。 */
public class ProductBasicPopulator implements Populator<ProductModel, ProductData>
{
	@Override
	public void populate(final ProductModel source, final ProductData target)
	{
		target.setCode(source.getCode());
		target.setName(source.getName());
		target.setSummary(source.getSummary());
		target.setDescription(source.getDescription());
		target.setManufacturer(source.getManufacturerName());
		target.setEan(source.getEan());
		target.setPurchasable(source.getApprovalStatus() == ArticleApprovalStatus.APPROVED);
		target.setUrl("/product/" + source.getCode());
	}
}
