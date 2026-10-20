package com.xyz.core.brand.impl

import com.xyz.core.brand.dao.BrandDao
import com.xyz.core.model.BrandModel
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.servicelayer.exceptions.AmbiguousIdentifierException
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException
import spock.lang.Specification

@UnitTest
class DefaultBrandServiceSpec extends Specification {

    def brandDao = Mock(BrandDao)
    def service = new DefaultBrandService(brandDao: brandDao)

    def "returns the only brand for a code"() {
        given:
        def brand = new BrandModel(code: "acme")
        brandDao.findBrandsByCode("acme") >> [brand]

        expect:
        service.getBrandForCode("acme").is(brand)
    }

    def "throws UnknownIdentifierException when no brand has the code"() {
        given:
        brandDao.findBrandsByCode("none") >> []

        when:
        service.getBrandForCode("none")

        then:
        thrown(UnknownIdentifierException)
    }

    def "throws AmbiguousIdentifierException when two brands have the code"() {
        given:
        brandDao.findBrandsByCode("dup") >> [new BrandModel(), new BrandModel()]

        when:
        service.getBrandForCode("dup")

        then:
        thrown(AmbiguousIdentifierException)
    }
}
