package com.xyz.facades.product.populators

import com.xyz.core.model.BrandModel
import com.xyz.facades.brand.data.BrandData
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.commercefacades.product.data.ProductData
import de.hybris.platform.core.model.product.ProductModel
import de.hybris.platform.servicelayer.dto.converter.Converter
import spock.lang.Specification

@UnitTest
class XyzProductBrandPopulatorSpec extends Specification {

    def brandConverter = Mock(Converter)
    def populator = new XyzProductBrandPopulator(brandConverter: brandConverter)

    def "copies an active brand"() {
        given:
        def brand = new BrandModel(code: "acme", active: true)
        def product = Spy(ProductModel) { getXyzNewArrival() >> true }
        product.brand = brand
        def brandData = new BrandData(code: "acme")
        def target = new ProductData()

        when:
        populator.populate(product, target)

        then:
        1 * brandConverter.convert(brand) >> brandData
        target.brand.is(brandData)
        target.newArrival
    }

    def "does not copy an inactive brand"() {
        given:
        def product = Spy(ProductModel) { getXyzNewArrival() >> false }
        product.brand = new BrandModel(code: "old", active: false)
        def target = new ProductData()

        when:
        populator.populate(product, target)

        then:
        0 * brandConverter.convert(_)
        target.brand == null
    }
}
