package de.hybris.platform.lite.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authorization: Bearer のトークンを読み、権限(ROLE_...)を付ける。無効なトークンは 401 の InvalidTokenError。 */
public class BearerTokenFilter extends OncePerRequestFilter
{
	private final OAuthTokenStore tokenStore;

	public BearerTokenFilter(final OAuthTokenStore tokenStore)
	{
		this.tokenStore = tokenStore;
	}

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
			throws ServletException, IOException
	{
		final String header = request.getHeader("Authorization");
		if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7))
		{
			final OAuthTokenStore.Token token = tokenStore.find(header.substring(7).trim());
			if (token == null)
			{
				response.setStatus(401);
				response.setContentType("application/json");
				response.getOutputStream().write(("{\"errors\":[{\"message\":\"Invalid access token: " + header.substring(7).trim()
						+ "\",\"type\":\"InvalidTokenError\"}]}").getBytes(StandardCharsets.UTF_8));
				return;
			}
			final var auth = new UsernamePasswordAuthenticationToken(token.userId() != null ? token.userId() : token.clientId(), null,
					token.authorities().stream().map(SimpleGrantedAuthority::new).toList());
			auth.setDetails(token);
			SecurityContextHolder.getContext().setAuthentication(auth);
		}
		chain.doFilter(request, response);
	}
}
