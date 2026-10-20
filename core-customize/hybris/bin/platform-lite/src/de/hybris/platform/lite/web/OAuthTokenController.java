package de.hybris.platform.lite.web;

import de.hybris.platform.core.model.security.PrincipalGroupModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.user.UserService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /authorizationserver/oauth/token。本物と同じ 2 つの流れに対応する:
 * <ul>
 * <li>grant_type=password(client_id=mobile_android、username、password)… 顧客のトークン(ROLE_CUSTOMERGROUP など)</li>
 * <li>grant_type=client_credentials(client_id=trusted_client)… 信頼されたクライアントのトークン(ROLE_TRUSTED_CLIENT)</li>
 * </ul>
 */
@RestController
public class OAuthTokenController
{
	private final OAuthTokenStore tokenStore;
	private final UserService userService;
	private final String clientSecret;

	public OAuthTokenController(final OAuthTokenStore tokenStore, final UserService userService,
			@Value("${oauth.client.mobile_android.secret:secret}") final String clientSecret)
	{
		this.tokenStore = tokenStore;
		this.userService = userService;
		this.clientSecret = clientSecret;
	}

	@PostMapping("/authorizationserver/oauth/token")
	public ResponseEntity<Map<String, Object>> token(@RequestParam("grant_type") final String grantType,
			@RequestParam("client_id") final String clientId, @RequestParam(value = "client_secret", required = false) final String secret,
			@RequestParam(value = "username", required = false) final String username,
			@RequestParam(value = "password", required = false) final String password)
	{
		if (!List.of("mobile_android", "trusted_client").contains(clientId) || !clientSecret.equals(secret))
		{
			return error(HttpStatus.UNAUTHORIZED, "invalid_client", "Bad client credentials");
		}
		final OAuthTokenStore.Token token;
		if ("password".equals(grantType))
		{
			final UserModel user;
			try
			{
				user = userService.getUserForUID(username == null ? "" : username.toLowerCase(Locale.ROOT));
			}
			catch (final UnknownIdentifierException e)
			{
				return error(HttpStatus.BAD_REQUEST, "invalid_grant", "Bad credentials");
			}
			if (user.isLoginDisabled() || !userService.checkPassword(user, password))
			{
				return error(HttpStatus.BAD_REQUEST, "invalid_grant", "Bad credentials");
			}
			final List<String> roles = new ArrayList<>(List.of("ROLE_CLIENT"));
			for (final PrincipalGroupModel g : user.getGroups())
			{
				roles.add("ROLE_" + g.getUid().toUpperCase(Locale.ROOT));
			}
			token = tokenStore.issue(clientId, user.getUid(), roles);
		}
		else if ("client_credentials".equals(grantType))
		{
			token = tokenStore.issue(clientId, null,
					"trusted_client".equals(clientId) ? List.of("ROLE_CLIENT", "ROLE_TRUSTED_CLIENT") : List.of("ROLE_CLIENT"));
		}
		else
		{
			return error(HttpStatus.BAD_REQUEST, "unsupported_grant_type", "Unsupported grant type: " + grantType);
		}
		final Map<String, Object> body = new LinkedHashMap<>();
		body.put("access_token", token.value());
		body.put("token_type", "bearer");
		body.put("expires_in", tokenStore.getValiditySeconds());
		body.put("scope", "basic openid");
		return ResponseEntity.ok(body);
	}

	private static ResponseEntity<Map<String, Object>> error(final HttpStatus status, final String error, final String description)
	{
		return ResponseEntity.status(status).body(Map.of("error", error, "error_description", description));
	}
}
