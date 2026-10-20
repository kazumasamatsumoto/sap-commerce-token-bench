package de.hybris.platform.commercewebservices.core.v2.controller;

import de.hybris.platform.commercefacades.product.ProductFacade;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.product.data.ReviewData;
import de.hybris.platform.commercefacades.search.ProductSearchFacade;
import de.hybris.platform.commercewebservices.core.exceptions.RequestParameterException;
import de.hybris.platform.commercewebservicescommons.dto.product.ProductWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.product.ReviewListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.product.ReviewWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.product.StockWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.search.SuggestionListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.search.SuggestionWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.search.facetdata.ProductSearchPageWsDTO;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/{baseSiteId}/products")
public class ProductsController extends BaseController
{
	@Resource(name = "productFacade")
	private ProductFacade productFacade;

	@Resource(name = "productSearchFacade")
	private ProductSearchFacade productSearchFacade;

	/** GET /{baseSiteId}/products/search?query=&currentPage=&pageSize=&sort=&fields= */
	@GetMapping("/search")
	public ProductSearchPageWsDTO searchProducts(@RequestParam(required = false) final String query,
			@RequestParam(defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage, @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@RequestParam(required = false) final String sort, @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		return getDataMapper().map(productSearchFacade.textSearch(query, currentPage, pageSize(pageSize), sort), ProductSearchPageWsDTO.class, fields);
	}

	/** GET /{baseSiteId}/products/suggestions?term=&max= */
	@GetMapping("/suggestions")
	public SuggestionListWsDTO getSuggestions(@RequestParam final String term, @RequestParam(defaultValue = "10") final int max)
	{
		final SuggestionListWsDTO list = new SuggestionListWsDTO();
		list.setSuggestions(productSearchFacade.getAutocompleteSuggestions(term, max).stream().map(v -> {
			final SuggestionWsDTO s = new SuggestionWsDTO();
			s.setValue(v);
			return s;
		}).toList());
		return list;
	}

	/** GET /{baseSiteId}/products/{productCode} */
	@GetMapping("/{productCode}")
	public ProductWsDTO getProductByCode(@PathVariable final String productCode, @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		return getDataMapper().map(productFacade.getProductForCode(productCode), ProductWsDTO.class, fields);
	}

	/** GET /{baseSiteId}/products/{productCode}/stock */
	@GetMapping("/{productCode}/stock")
	public StockWsDTO getStockData(@PathVariable final String productCode, @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final ProductData product = productFacade.getProductForCode(productCode);
		return getDataMapper().map(product.getStock(), StockWsDTO.class, fields);
	}

	/** GET /{baseSiteId}/products/{productCode}/reviews?maxCount= */
	@GetMapping("/{productCode}/reviews")
	public ReviewListWsDTO getProductReviews(@PathVariable final String productCode, @RequestParam(required = false) final Integer maxCount,
			@RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		List<ReviewData> reviews = productFacade.getReviews(productCode);
		if (maxCount != null && reviews.size() > maxCount)
		{
			reviews = reviews.subList(0, maxCount);
		}
		final ReviewListWsDTO list = new ReviewListWsDTO();
		list.setReviews(reviews.stream().map(r -> getDataMapper().map(r, ReviewWsDTO.class, fields)).toList());
		return list;
	}

	/** POST /{baseSiteId}/products/{productCode}/reviews … ログインした人だけ。rating は 1〜5。 */
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT" })
	@PostMapping("/{productCode}/reviews")
	@ResponseStatus(HttpStatus.CREATED)
	public ReviewWsDTO createProductReview(@PathVariable final String productCode, @RequestBody final ReviewWsDTO review,
			@RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5)
		{
			throw new RequestParameterException("rating must be between 1 and 5", "rating");
		}
		if (review.getHeadline() == null || review.getHeadline().isBlank())
		{
			throw new RequestParameterException("headline is required", "headline");
		}
		final ReviewData data = getDataMapper().mapToData(review, ReviewData.class);
		return getDataMapper().map(productFacade.postReview(productCode, data), ReviewWsDTO.class, fields);
	}
}
