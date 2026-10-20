package de.hybris.platform.commercefacades.product.converters.populators;

import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.customerreview.CustomerReviewService;

/** 平均の評価とレビューの数。平均は商品に持っている値(averageRating)を優先する。 */
public class ProductReviewSummaryPopulator implements Populator<ProductModel, ProductData>
{
	private CustomerReviewService customerReviewService;

	@Override
	public void populate(final ProductModel source, final ProductData target)
	{
		target.setAverageRating(source.getAverageRating() != null ? source.getAverageRating() : customerReviewService.getAverageRating(source));
		target.setNumberOfReviews(customerReviewService.getNumberOfReviews(source));
	}

	public void setCustomerReviewService(final CustomerReviewService customerReviewService)
	{
		this.customerReviewService = customerReviewService;
	}
}
