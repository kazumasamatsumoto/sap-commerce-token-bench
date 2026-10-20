package com.xyz.core.attributehandlers

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.core.model.product.ProductModel
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@UnitTest
class XyzNewArrivalAttributeHandlerSpec extends Specification {

    def now = Instant.parse("2026-10-20T00:00:00Z")
    def handler = new XyzNewArrivalAttributeHandler(clock: Clock.fixed(now, ZoneOffset.UTC), newArrivalDays: 30)

    @Unroll
    def "launch #daysAgo days ago -> newArrival is #expected"() {
        given:
        def product = new ProductModel()
        product.xyzLaunchDate = daysAgo == null ? null : Date.from(now.minusSeconds(daysAgo * 86400L))

        expect:
        handler.get(product) == expected

        where:
        daysAgo || expected
        null    || false
        0       || true
        29      || true
        30      || false
        -1      || false
    }
}
