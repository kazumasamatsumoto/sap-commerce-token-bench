package de.hybris.platform.lite

import de.hybris.platform.catalog.model.CatalogModel
import de.hybris.platform.catalog.model.CatalogVersionModel
import de.hybris.platform.category.model.CategoryModel
import de.hybris.platform.catalog.enums.ArticleApprovalStatus
import de.hybris.platform.core.model.c2l.CurrencyModel
import de.hybris.platform.core.model.order.CartEntryModel
import de.hybris.platform.core.model.order.CartModel
import de.hybris.platform.core.model.product.ProductModel
import de.hybris.platform.europe1.model.PriceRowModel
import de.hybris.platform.servicelayer.exceptions.ModelSavingException
import de.hybris.platform.servicelayer.model.ModelService
import de.hybris.platform.servicelayer.search.FlexibleSearchService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification
import spock.lang.Tag

/**
 * 疑似プラットフォームの保存と検索を、DB を使って確かめる。ant initialize の後に ant integrationtests で回す。
 */
@Tag("integration")
@SpringBootTest(classes = HybrisLiteApplication, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ModelServiceIntegrationSpec extends Specification {

    @Autowired
    ModelService modelService

    @Autowired
    FlexibleSearchService flexibleSearchService

    String suffix = UUID.randomUUID().toString().substring(0, 8)

    CatalogVersionModel newCatalogVersion() {
        def catalog = modelService.create(CatalogModel)
        catalog.id = "itCatalog-" + suffix
        def cv = modelService.create(CatalogVersionModel)
        cv.catalog = catalog
        cv.version = "Staged"
        modelService.save(cv)
        cv
    }

    def "saves a product with localized names, an enum, categories and prices, and reads it back"() {
        given:
        def cv = newCatalogVersion()
        def category = modelService.create(CategoryModel)
        category.code = "cat-" + suffix
        category.catalogVersion = cv
        def product = modelService.create(ProductModel)
        product.code = "p-" + suffix
        product.catalogVersion = cv
        product.setName("テスト商品", Locale.JAPANESE)
        product.setName("Test product", Locale.ENGLISH)
        product.approvalStatus = ArticleApprovalStatus.APPROVED
        product.supercategories = [category]
        def jpy = flexibleSearchService.search("SELECT {pk} FROM {Currency} WHERE {isocode} = ?c", [c: "JPY"]).result[0] as CurrencyModel
        def price = modelService.create(PriceRowModel)
        price.price = 1980d
        price.currency = jpy
        product.europe1Prices = [price]

        when:
        modelService.save(product)
        def found = flexibleSearchService.search(
                "SELECT {p.pk} FROM {Product AS p JOIN CatalogVersion AS cv ON {p.catalogVersion} = {cv.pk}} WHERE {p.code} = ?code AND {cv.version} = 'Staged'",
                [code: product.code]).result

        then:
        found.size() == 1
        with(found[0] as ProductModel) {
            getName(Locale.JAPANESE) == "テスト商品"
            getName(Locale.ENGLISH) == "Test product"
            approvalStatus == ArticleApprovalStatus.APPROVED
            catalogVersion.catalog.id == "itCatalog-" + suffix
            supercategories*.code == ["cat-" + suffix]
            europe1Prices*.price == [1980d]
            europe1Prices[0].currency.isocode == "JPY"
        }

        and: "the category sees the product through the many-to-many relation"
        def cat = flexibleSearchService.search("SELECT {pk} FROM {Category} WHERE {code} = ?c", [c: category.code]).result[0] as CategoryModel
        cat.products*.code == [product.code]

        and: "localized attributes can be searched in a given language"
        flexibleSearchService.search("SELECT {pk} FROM {Product} WHERE {name[en]} = 'Test product' AND {code} = ?c", [c: product.code]).result.size() == 1
    }

    def "keeps the order entries of a cart (one-to-many)"() {
        given:
        def cv = newCatalogVersion()
        def product = modelService.create(ProductModel)
        product.code = "p2-" + suffix
        product.catalogVersion = cv
        def cart = modelService.create(CartModel)
        cart.code = "cart-" + suffix
        def e1 = modelService.create(CartEntryModel)
        e1.entryNumber = 0
        e1.product = product
        e1.quantity = 2L
        def e2 = modelService.create(CartEntryModel)
        e2.entryNumber = 1
        e2.product = product
        e2.quantity = 1L
        cart.entries = [e1, e2]

        when:
        modelService.save(cart)
        def reloaded = flexibleSearchService.search("SELECT {pk} FROM {Cart} WHERE {code} = ?c", [c: cart.code]).result[0] as CartModel

        then:
        reloaded.entries*.quantity == [2L, 1L]
        reloaded.entries.every { it.order.code == cart.code }
        flexibleSearchService.search("SELECT {pk} FROM {AbstractOrder} WHERE {code} = ?c", [c: cart.code]).result.size() == 1
        flexibleSearchService.search("SELECT {pk} FROM {Order} WHERE {code} = ?c", [c: cart.code]).result.isEmpty()
    }

    def "refuses to save without a mandatory attribute"() {
        given:
        def product = modelService.create(ProductModel)
        product.code = "p3-" + suffix

        when:
        modelService.save(product)

        then:
        def e = thrown(ModelSavingException)
        e.message.contains("missing values for [catalogVersion]")
    }

    def "refuses a second item with the same unique key"() {
        given:
        def cv = newCatalogVersion()
        def first = modelService.create(ProductModel)
        first.code = "dup-" + suffix
        first.catalogVersion = cv
        modelService.save(first)
        def second = modelService.create(ProductModel)
        second.code = "dup-" + suffix
        second.catalogVersion = cv

        when:
        modelService.save(second)

        then:
        def e = thrown(ModelSavingException)
        e.message.contains("ambiguous unique keys")
    }
}
