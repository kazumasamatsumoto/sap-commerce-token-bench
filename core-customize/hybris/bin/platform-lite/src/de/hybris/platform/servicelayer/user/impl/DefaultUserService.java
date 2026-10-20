package de.hybris.platform.servicelayer.user.impl;

import de.hybris.platform.core.model.security.PrincipalGroupModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.core.model.user.UserGroupModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.internal.dao.AbstractItemDao;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.user.UserService;
import java.util.List;
import java.util.Map;

public class DefaultUserService extends AbstractItemDao implements UserService
{
	public static final String ANONYMOUS_UID = "anonymous";
	private static final String CURRENT_USER = "user";

	private SessionService sessionService;

	@Override
	public UserModel getUserForUID(final String userId)
	{
		final List<UserModel> list = find("SELECT {pk} FROM {User} WHERE {uid} = ?uid", Map.of("uid", userId.toLowerCase()));
		if (list.isEmpty())
		{
			throw new UnknownIdentifierException("Cannot find user with uid '" + userId + "'");
		}
		return list.get(0);
	}

	@Override
	public <T extends UserModel> T getUserForUID(final String userId, final Class<T> clazz)
	{
		final UserModel u = getUserForUID(userId);
		if (!clazz.isInstance(u))
		{
			throw new IllegalArgumentException("user " + userId + " is not a " + clazz.getSimpleName());
		}
		return clazz.cast(u);
	}

	@Override
	public boolean isUserExisting(final String uid)
	{
		return !find("SELECT {pk} FROM {User} WHERE {uid} = ?uid", Map.of("uid", uid.toLowerCase())).isEmpty();
	}

	@Override
	public UserGroupModel getUserGroupForUID(final String groupId)
	{
		return findUnique("SELECT {pk} FROM {UserGroup} WHERE {uid} = ?uid", Map.of("uid", groupId), "UserGroup with uid '" + groupId + "'");
	}

	@Override
	public UserModel getCurrentUser()
	{
		final UserModel u = sessionService.getAttribute(CURRENT_USER);
		return u != null ? u : getAnonymousUser();
	}

	@Override
	public void setCurrentUser(final UserModel user)
	{
		sessionService.setAttribute(CURRENT_USER, user);
	}

	@Override
	public CustomerModel getAnonymousUser()
	{
		return getUserForUID(ANONYMOUS_UID, CustomerModel.class);
	}

	@Override
	public boolean isAnonymousUser(final UserModel user)
	{
		return user == null || ANONYMOUS_UID.equals(user.getUid());
	}

	@Override
	public boolean isMemberOfGroup(final UserModel member, final UserGroupModel group)
	{
		for (final PrincipalGroupModel g : member.getGroups())
		{
			if (g.getUid().equals(group.getUid()))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean checkPassword(final UserModel user, final String plainPassword)
	{
		final String encoded = user.getEncodedPassword();
		return encoded != null && plainPassword != null && encoded.equals("{noop}" + plainPassword);
	}

	@Override
	public void setPassword(final UserModel user, final String plainPassword)
	{
		user.setEncodedPassword("{noop}" + plainPassword);
	}

	public void setSessionService(final SessionService sessionService)
	{
		this.sessionService = sessionService;
	}
}
