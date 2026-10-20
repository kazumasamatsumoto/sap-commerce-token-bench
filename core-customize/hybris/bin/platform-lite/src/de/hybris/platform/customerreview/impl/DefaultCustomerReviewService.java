package de.hybris.platform.customerreview.impl;

import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.customerreview.CustomerReviewService;
import de.hybris.platform.customerreview.model.CustomerReviewModel;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import java.util.List;
import java.util.Map;

public class DefaultCustomerReviewService extends AbstractItemDao implements CustomerReviewService
{
	@Override
	public List<CustomerReviewModel> getReviewsForProduct(final ProductModel product)
	{
		return find("SELECT {pk} FROM {CustomerReview} WHERE {product} = ?product ORDER BY {creationtime} DESC", Map.of("product", product));
	}

	@Override
	public CustomerReviewModel createCustomerReview(final Double rating, final String headline, final String comment, final UserModel user,
			final ProductModel product)
	{
		final CustomerReviewModel review = getModelService().create(CustomerReviewModel.class);
		review.setRating(rating);
		review.setHeadline(headline);
		review.setComment(comment);
		review.setUser(user);
		review.setAlias(user.getName());
		review.setProduct(product);
		getModelService().save(review);
		return review;
	}

	@Override
	public Double getAverageRating(final ProductModel product)
	{
		final List<Double> r = getFlexibleSearchService().<Double> search(
				new de.hybris.platform.servicelayer.search.FlexibleSearchQuery("SELECT avg({rating}) FROM {CustomerReview} WHERE {product} = ?p",
						Map.of("p", product)) {
					{
						setResultClassList(List.of(Double.class));
					}
				}).getResult();
		return r.isEmpty() ? null : r.get(0);
	}

	@Override
	public Integer getNumberOfReviews(final ProductModel product)
	{
		final de.hybris.platform.servicelayer.search.FlexibleSearchQuery q = new de.hybris.platform.servicelayer.search.FlexibleSearchQuery(
				"SELECT count({pk}) FROM {CustomerReview} WHERE {product} = ?p", Map.of("p", product));
		q.setResultClassList(List.of(Integer.class));
		final List<Integer> r = getFlexibleSearchService().<Integer> search(q).getResult();
		return r.isEmpty() ? 0 : r.get(0);
	}
}
