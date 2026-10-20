package de.hybris.platform.lite.config;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hybris.platform.cmsoccaddon.data.ComponentWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.search.facetdata.ProductSearchPageWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.search.pagedata.PaginationWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.user.UserWsDTO;
import de.hybris.platform.lite.web.OAuthTokenStore;
import de.hybris.platform.webservicescommons.mapping.config.FieldSetLevelMapping;
import de.hybris.platform.webservicescommons.mapping.impl.DefaultDataMapper;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OCC で使う bean のうち、ant initialize など(サーバーでないとき)にも作っておくもの。 */
@Configuration(proxyBeanMethods = false)
public class OccCommonConfiguration
{
	/** CMS のコンポーネントは、otherProperties の中身をコンポーネントの項目として平たく出す(本物と同じ形)。 */
	abstract static class ComponentWsDTOMixin
	{
		@JsonAnyGetter
		abstract Map<String, Object> getOtherProperties();
	}

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer occJacksonCustomizer()
	{
		return b -> b.mixIn(ComponentWsDTO.class, ComponentWsDTOMixin.class);
	}

	@Bean(name = "dataMapper")
	public DefaultDataMapper dataMapper(final ObjectMapper objectMapper, final ObjectProvider<FieldSetLevelMapping> mappings)
	{
		final DefaultDataMapper mapper = new DefaultDataMapper(objectMapper, mappings);
		mapper.addRename(ProductSearchPageWsDTO.class, "results", "products");
		mapper.addRename(PaginationWsDTO.class, "totalNumberOfResults", "totalResults");
		mapper.addRename(PaymentDetailsWsDTO.class, "defaultPaymentInfo", "defaultPayment");
		mapper.addRename(UserWsDTO.class, "defaultShippingAddress", "defaultAddress");
		return mapper;
	}

	@Bean
	public OAuthTokenStore oauthTokenStore(@Value("${oauth.token.validity.seconds:43200}") final long validitySeconds)
	{
		return new OAuthTokenStore(validitySeconds);
	}

}
