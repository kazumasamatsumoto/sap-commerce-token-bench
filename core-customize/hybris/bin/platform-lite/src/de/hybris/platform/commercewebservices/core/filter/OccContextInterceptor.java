package de.hybris.platform.commercewebservices.core.filter;

import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.commercewebservices.core.exceptions.InvalidResourceException;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.site.BaseSiteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 本物の BaseSiteMatchingFilter / UserMatchingFilter / SessionLanguageFilter / SessionCurrencyFilter をまとめたもの。
 * <ul>
 * <li>{baseSiteId} → 今のサイト(とカタログ)。無いサイトは 400 の InvalidResourceError</li>
 * <li>?lang= / ?curr= → 今の言語と通貨</li>
 * <li>{userId}: current = トークンの人、anonymous = ゲスト、それ以外の uid は本人か ROLE_TRUSTED_CLIENT だけ</li>
 * </ul>
 */
public class OccContextInterceptor implements HandlerInterceptor
{
	private final BaseSiteService baseSiteService;
	private final UserService userService;
	private final CommonI18NService commonI18NService;
	private final SessionService sessionService;

	public OccContextInterceptor(final BaseSiteService baseSiteService, final UserService userService, final CommonI18NService commonI18NService,
			final SessionService sessionService)
	{
		this.baseSiteService = baseSiteService;
		this.userService = userService;
		this.commonI18NService = commonI18NService;
		this.sessionService = sessionService;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler)
	{
		sessionService.closeCurrentSession();
		final Map<String, String> vars = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		final String siteId = vars == null ? null : vars.get("baseSiteId");
		if (siteId != null)
		{
			final BaseSiteModel site = baseSiteService.getBaseSiteForUID(siteId);
			if (site == null)
			{
				throw new InvalidResourceException("Base site " + siteId + " doesn't exist", siteId);
			}
			baseSiteService.setCurrentBaseSite(site, true);
		}
		final String lang = request.getParameter("lang");
		if (lang != null && !lang.isBlank())
		{
			commonI18NService.setCurrentLanguage(commonI18NService.getLanguage(lang));
		}
		else if (siteId != null && baseSiteService.getCurrentBaseSite().getDefaultLanguage() != null)
		{
			commonI18NService.setCurrentLanguage(baseSiteService.getCurrentBaseSite().getDefaultLanguage());
		}
		final String curr = request.getParameter("curr");
		if (curr != null && !curr.isBlank())
		{
			commonI18NService.setCurrentCurrency(commonI18NService.getCurrency(curr));
		}
		else if (siteId != null && !baseSiteService.getCurrentBaseSite().getStores().isEmpty()
				&& baseSiteService.getCurrentBaseSite().getStores().get(0).getDefaultCurrency() != null)
		{
			commonI18NService.setCurrentCurrency(baseSiteService.getCurrentBaseSite().getStores().get(0).getDefaultCurrency());
		}
		userService.setCurrentUser(resolveUser(vars == null ? null : vars.get("userId")));
		return true;
	}

	private UserModel resolveUser(final String userId)
	{
		final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		final String tokenUser = auth != null && auth.getDetails() instanceof de.hybris.platform.lite.web.OAuthTokenStore.Token t ? t.userId() : null;
		final boolean trusted = auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_TRUSTED_CLIENT".equals(a.getAuthority()));
		if (userId == null)
		{
			return tokenUser != null ? userService.getUserForUID(tokenUser) : userService.getAnonymousUser();
		}
		if ("anonymous".equals(userId))
		{
			return userService.getAnonymousUser();
		}
		if ("current".equals(userId))
		{
			if (tokenUser == null)
			{
				throw new InsufficientAuthenticationException("Full authentication is required to access this resource");
			}
			return userService.getUserForUID(tokenUser);
		}
		if (trusted || userId.equalsIgnoreCase(tokenUser))
		{
			try
			{
				return userService.getUserForUID(userId);
			}
			catch (final UnknownIdentifierException e)
			{
				throw new UnknownIdentifierException("Cannot find user with propertyValue '" + userId + "'");
			}
		}
		if (tokenUser == null)
		{
			throw new InsufficientAuthenticationException("Full authentication is required to access this resource");
		}
		throw new AccessDeniedException("Access is denied");
	}

	@Override
	public void afterCompletion(final HttpServletRequest request, final HttpServletResponse response, final Object handler, final Exception ex)
	{
		sessionService.closeCurrentSession();
		de.hybris.platform.core.Registry.getApplicationContext().getBean(de.hybris.platform.servicelayer.i18n.I18NService.class).setCurrentLocale(null);
	}
}
