package de.hybris.platform.commercefacades.product.impl;

import de.hybris.platform.commercefacades.product.ProductFacade;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.product.data.ReviewData;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.customerreview.CustomerReviewService;
import de.hybris.platform.customerreview.model.CustomerReviewModel;
import de.hybris.platform.product.ProductService;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.servicelayer.user.UserService;
import java.util.List;

public class DefaultProductFacade implements ProductFacade
{
	private ProductService productService;
	private CustomerReviewService customerReviewService;
	private UserService userService;
	private Converter<ProductModel, ProductData> productConverter;
	private Converter<CustomerReviewModel, ReviewData> customerReviewConverter;

	@Override
	public ProductData getProductForCode(final String code)
	{
		return productConverter.convert(productService.getProductForCode(code));
	}

	@Override
	public List<ReviewData> getReviews(final String productCode)
	{
		return customerReviewConverter.convertAll(customerReviewService.getReviewsForProduct(productService.getProductForCode(productCode)));
	}

	@Override
	public ReviewData postReview(final String productCode, final ReviewData review)
	{
		final ProductModel product = productService.getProductForCode(productCode);
		final CustomerReviewModel model = customerReviewService.createCustomerReview(review.getRating(), review.getHeadline(), review.getComment(),
				userService.getCurrentUser(), product);
		if (review.getAlias() != null)
		{
			model.setAlias(review.getAlias());
		}
		return customerReviewConverter.convert(model);
	}

	public void setProductService(final ProductService productService)
	{
		this.productService = productService;
	}

	public void setCustomerReviewService(final CustomerReviewService customerReviewService)
	{
		this.customerReviewService = customerReviewService;
	}

	public void setUserService(final UserService userService)
	{
		this.userService = userService;
	}

	public void setProductConverter(final Converter<ProductModel, ProductData> productConverter)
	{
		this.productConverter = productConverter;
	}

	public void setCustomerReviewConverter(final Converter<CustomerReviewModel, ReviewData> customerReviewConverter)
	{
		this.customerReviewConverter = customerReviewConverter;
	}
}
