package de.hybris.bootstrap.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;

/** DB と Spring を使うテストの印。ant unittests では回らず、ant integrationtests で回る(先に ant initialize が要る)。 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Tag("integration")
public @interface IntegrationTest
{
}
