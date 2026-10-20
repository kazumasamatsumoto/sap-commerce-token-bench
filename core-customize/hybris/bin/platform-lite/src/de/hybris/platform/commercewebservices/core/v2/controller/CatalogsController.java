package de.hybris.platform.commercewebservices.core.v2.controller;

import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.category.CategoryService;
import de.hybris.platform.category.model.CategoryModel;
import de.hybris.platform.commercefacades.search.ProductSearchFacade;
import de.hybris.platform.commercewebservicescommons.dto.catalog.CatalogListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.catalog.CatalogVersionWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.catalog.CatalogWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.catalog.CategoryHierarchyWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.search.facetdata.ProductSearchPageWsDTO;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/{baseSiteId}")
public class CatalogsController extends BaseController
{
	@Resource(name = "catalogVersionService")
	private CatalogVersionService catalogVersionService;

	@Resource(name = "categoryService")
	private CategoryService categoryService;

	@Resource(name = "productSearchFacade")
	private ProductSearchFacade productSearchFacade;

	/** GET /{baseSiteId}/catalogs … 今のサイトの商品カタログと、カテゴリの木(2 段)。 */
	@GetMapping("/catalogs")
	public CatalogListWsDTO getCatalogs()
	{
		final List<CatalogWsDTO> catalogs = new ArrayList<>();
		for (final CatalogVersionModel cv : catalogVersionService.getSessionCatalogVersions())
		{
			final List<CategoryModel> roots = categoryService.getRootCategoriesForCatalogVersion(cv);
			if (roots.isEmpty())
			{
				continue; // コンテンツのカタログ(カテゴリが無い)は出さない
			}
			final CatalogWsDTO c = new CatalogWsDTO();
			c.setId(cv.getCatalog().getId());
			c.setName(cv.getCatalog().getName());
			final CatalogVersionWsDTO v = new CatalogVersionWsDTO();
			v.setId(cv.getVersion());
			v.setUrl("/" + c.getId() + "/" + cv.getVersion());
			v.setCategories(roots.stream().map(r -> hierarchy(r, cv, 1)).toList());
			c.setCatalogVersions(List.of(v));
			catalogs.add(c);
		}
		final CatalogListWsDTO list = new CatalogListWsDTO();
		list.setCatalogs(catalogs);
		return list;
	}

	/** GET /{baseSiteId}/catalogs/{catalogId}/{catalogVersionId}/categories/{categoryId} */
	@GetMapping("/catalogs/{catalogId}/{catalogVersionId}/categories/{categoryId}")
	public CategoryHierarchyWsDTO getCategory(@PathVariable final String catalogId, @PathVariable final String catalogVersionId,
			@PathVariable final String categoryId)
	{
		final CatalogVersionModel cv = catalogVersionService.getCatalogVersion(catalogId, catalogVersionId);
		return hierarchy(categoryService.getCategoryForCode(categoryId), cv, 2);
	}

	/** GET /{baseSiteId}/categories/{categoryId}/products … カテゴリの商品の一覧(検索と同じ形)。 */
	@GetMapping("/categories/{categoryId}/products")
	public ProductSearchPageWsDTO searchByCategory(@PathVariable final String categoryId,
			@RequestParam(defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage, @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@RequestParam(required = false) final String sort, @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		categoryService.getCategoryForCode(categoryId);
		return getDataMapper().map(productSearchFacade.textSearch(":relevance:category:" + categoryId, currentPage, pageSize(pageSize), sort),
				ProductSearchPageWsDTO.class, fields);
	}

	private CategoryHierarchyWsDTO hierarchy(final CategoryModel category, final CatalogVersionModel cv, final int depth)
	{
		final CategoryHierarchyWsDTO h = new CategoryHierarchyWsDTO();
		h.setId(category.getCode());
		h.setName(category.getName());
		h.setUrl("/" + cv.getCatalog().getId() + "/" + cv.getVersion() + "/categories/" + category.getCode());
		h.setSubcategories(depth <= 0 ? List.of() : category.getCategories().stream().map(c -> hierarchy(c, cv, depth - 1)).toList());
		return h;
	}
}
