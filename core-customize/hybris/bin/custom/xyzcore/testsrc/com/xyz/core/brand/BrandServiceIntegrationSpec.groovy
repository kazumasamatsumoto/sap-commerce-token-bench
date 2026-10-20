package com.xyz.core.brand

import com.xyz.core.enums.BrandTier
import com.xyz.core.model.BrandModel
import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.servicelayer.ServicelayerTransactionalSpockSpecification
import de.hybris.platform.servicelayer.exceptions.ModelSavingException
import de.hybris.platform.servicelayer.model.ModelService
import jakarta.annotation.Resource
import spock.lang.Tag

@IntegrationTest
@Tag("integration")
class BrandServiceIntegrationSpec extends ServicelayerTransactionalSpockSpecification {

    @Resource
    ModelService modelService

    @Resource
    BrandService brandService

    def "finds a saved brand by code, with the default tier"() {
        given:
        def brand = modelService.create(BrandModel)
        brand.code = "it-brand"
        brand.setName("テストブランド", Locale.JAPANESE)
        modelService.save(brand)

        when:
        def found = brandService.getBrandForCode("it-brand")

        then:
        found.getName(Locale.JAPANESE) == "テストブランド"
        found.tier == BrandTier.STANDARD
        found.active
    }

    def "refuses a brand code with upper case letters (BrandValidateInterceptor)"() {
        given:
        def brand = modelService.create(BrandModel)
        brand.code = "Bad Code"

        when:
        modelService.save(brand)

        then:
        def e = thrown(ModelSavingException)
        e.message.contains("must match")
    }
}
