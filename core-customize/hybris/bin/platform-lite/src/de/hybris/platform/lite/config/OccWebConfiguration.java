package de.hybris.platform.lite.config;

import de.hybris.platform.commercewebservices.core.filter.OccContextInterceptor;
import de.hybris.platform.lite.web.BearerTokenFilter;
import de.hybris.platform.lite.web.OAuthTokenStore;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.site.BaseSiteService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * OCC の土台(サーバーとして動くときだけ)。本物の commercewebservices と同じく、OCC の Controller はすべて /occ/v2 の下に出す
 * (Controller には /{baseSiteId}/products のように、/occ/v2 を除いた道を書く)。
 * /authorizationserver/oauth/token と /medias/ は /occ/v2 の外(de.hybris.platform.lite.web)。
 * @Secured は本物と同じ役の名前(ROLE_CUSTOMERGROUP、ROLE_TRUSTED_CLIENT、ROLE_CLIENT、ROLE_ANONYMOUS など)で書く。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = false)
@ComponentScan({ "de.hybris.platform.commercewebservices", "de.hybris.platform.lite.web" })
public class OccWebConfiguration implements WebMvcConfigurer
{
	public static final String OCC_PREFIX = "/occ/v2";

	private final BaseSiteService baseSiteService;
	private final UserService userService;
	private final CommonI18NService commonI18NService;
	private final SessionService sessionService;

	public OccWebConfiguration(@Lazy final BaseSiteService baseSiteService, @Lazy final UserService userService,
			@Lazy final CommonI18NService commonI18NService, @Lazy final SessionService sessionService)
	{
		this.baseSiteService = baseSiteService;
		this.userService = userService;
		this.commonI18NService = commonI18NService;
		this.sessionService = sessionService;
	}

	@Override
	public void configurePathMatch(final PathMatchConfigurer configurer)
	{
		configurer.addPathPrefix(OCC_PREFIX, HandlerTypePredicate.forAnnotation(Controller.class, RestController.class)
				.and(HandlerTypePredicate.forBasePackage("de.hybris.platform.lite.web").negate()));
	}

	@Override
	public void addInterceptors(final InterceptorRegistry registry)
	{
		registry.addInterceptor(new OccContextInterceptor(baseSiteService, userService, commonI18NService, sessionService))
				.addPathPatterns(OCC_PREFIX + "/**");
	}

	@Override
	public void addCorsMappings(final CorsRegistry registry)
	{
		registry.addMapping("/**").allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*").allowedMethods("*").allowedHeaders("*")
				.allowCredentials(true);
	}

	@Bean
	public SecurityFilterChain occSecurityFilterChain(final HttpSecurity http, final OAuthTokenStore tokenStore) throws Exception
	{
		http.csrf(c -> c.disable())
				.cors(c -> {
				})
				.httpBasic(b -> b.disable())
				.formLogin(f -> f.disable())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(new BearerTokenFilter(tokenStore), AnonymousAuthenticationFilter.class)
				.authorizeHttpRequests(a -> a.anyRequest().permitAll());
		return http.build();
	}
}
