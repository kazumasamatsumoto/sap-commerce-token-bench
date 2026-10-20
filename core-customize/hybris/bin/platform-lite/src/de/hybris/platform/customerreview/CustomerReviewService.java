package de.hybris.platform.customerreview;

import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.customerreview.model.CustomerReviewModel;
import java.util.List;

public interface CustomerReviewService
{
	List<CustomerReviewModel> getReviewsForProduct(ProductModel product);

	CustomerReviewModel createCustomerReview(Double rating, String headline, String comment, UserModel user, ProductModel product);

	Double getAverageRating(ProductModel product);

	Integer getNumberOfReviews(ProductModel product);
}
