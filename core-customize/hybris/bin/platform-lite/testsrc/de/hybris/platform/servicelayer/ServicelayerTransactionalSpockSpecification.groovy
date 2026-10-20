package de.hybris.platform.servicelayer

import de.hybris.platform.lite.HybrisLiteApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

/**
 * DB を使う Spock のテストの親。テストごとにトランザクションを巻き戻すので、作ったデータは残らない。
 * 子のクラスには @IntegrationTest と spock.lang.Tag("integration") を付ける(ant unittests から外すため)。
 */
@SpringBootTest(classes = HybrisLiteApplication, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
abstract class ServicelayerTransactionalSpockSpecification extends Specification {
}
