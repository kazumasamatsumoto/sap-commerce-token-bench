package de.hybris.platform.lite.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 発行したアクセストークンを覚えておく(疑似。メモリだけに持つので、サーバーを止めると消える)。
 * 本物の authorizationserver の代わり。
 */
public class OAuthTokenStore
{
	public record Token(String value, String clientId, String userId, List<String> authorities, Instant expiresAt)
	{
	}

	private final Map<String, Token> tokens = new ConcurrentHashMap<>();
	private final long validitySeconds;

	public OAuthTokenStore(final long validitySeconds)
	{
		this.validitySeconds = validitySeconds;
	}

	public Token issue(final String clientId, final String userId, final List<String> authorities)
	{
		final Token t = new Token(UUID.randomUUID().toString(), clientId, userId, authorities, Instant.now().plusSeconds(validitySeconds));
		tokens.put(t.value(), t);
		return t;
	}

	public Token find(final String value)
	{
		final Token t = tokens.get(value);
		if (t != null && t.expiresAt().isBefore(Instant.now()))
		{
			tokens.remove(value);
			return null;
		}
		return t;
	}

	public void revoke(final String value)
	{
		tokens.remove(value);
	}

	public long getValiditySeconds()
	{
		return validitySeconds;
	}
}
