package de.hybris.platform.commercefacades.product;

import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.product.data.ReviewData;
import java.util.List;

public interface ProductFacade
{
	ProductData getProductForCode(String code);

	List<ReviewData> getReviews(String productCode);

	ReviewData postReview(String productCode, ReviewData review);
}
