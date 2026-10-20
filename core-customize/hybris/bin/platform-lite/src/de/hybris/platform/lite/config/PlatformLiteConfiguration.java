package de.hybris.platform.lite.config;

import de.hybris.bootstrap.typesystem.TypeSystem;
import de.hybris.platform.core.Registry;
import de.hybris.platform.lite.impex.ImpExImporter;
import de.hybris.platform.lite.persistence.ItemPersistence;
import de.hybris.platform.lite.persistence.SchemaManager;
import de.hybris.platform.lite.runtime.TypeSystemHolder;
import de.hybris.platform.servicelayer.i18n.I18NService;
import de.hybris.platform.servicelayer.i18n.impl.DefaultI18NService;
import de.hybris.platform.servicelayer.interceptor.impl.InterceptorMapping;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.model.impl.DefaultModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.impl.DefaultFlexibleSearchService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 疑似プラットフォームの bean。名前は本物と同じにしてあるので、拡張の *-spring.xml から ref="modelService" のように引ける。
 */
@Configuration(proxyBeanMethods = false)
public class PlatformLiteConfiguration
{
	@Bean
	public TypeSystem typeSystem(final ApplicationContext ctx)
	{
		Registry.setApplicationContext(ctx);
		return TypeSystemHolder.get();
	}

	@Bean
	public ItemPersistence itemPersistence(final JdbcTemplate jdbcTemplate, final TypeSystem typeSystem)
	{
		return new ItemPersistence(jdbcTemplate, typeSystem);
	}

	@Bean
	public SchemaManager schemaManager(final JdbcTemplate jdbcTemplate, final TypeSystem typeSystem)
	{
		return new SchemaManager(jdbcTemplate, typeSystem);
	}

	@Bean(name = "i18NService")
	public I18NService i18NService(@Value("${default.session.language.isocode:ja}") final String defaultLanguage)
	{
		return new DefaultI18NService(defaultLanguage);
	}

	@Bean(name = "modelService")
	public ModelService modelService(final ItemPersistence itemPersistence, final ObjectProvider<InterceptorMapping> interceptorMappings,
			final PlatformTransactionManager txManager)
	{
		return new DefaultModelService(itemPersistence, interceptorMappings, new TransactionTemplate(txManager));
	}

	@Bean(name = "flexibleSearchService")
	public FlexibleSearchService flexibleSearchService(final JdbcTemplate jdbcTemplate, final ItemPersistence itemPersistence,
			final I18NService i18NService)
	{
		return new DefaultFlexibleSearchService(jdbcTemplate, itemPersistence, i18NService);
	}

	@Bean(name = "impExImporter")
	public ImpExImporter impExImporter(final ModelService modelService, final FlexibleSearchService flexibleSearchService,
			final ItemPersistence itemPersistence)
	{
		return new ImpExImporter(modelService, flexibleSearchService, itemPersistence);
	}
}
