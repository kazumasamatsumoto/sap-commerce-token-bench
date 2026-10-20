package de.hybris.platform.servicelayer.user;

import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.core.model.user.UserGroupModel;
import de.hybris.platform.core.model.user.UserModel;

public interface UserService
{
	UserModel getUserForUID(String userId);

	<T extends UserModel> T getUserForUID(String userId, Class<T> clazz);

	boolean isUserExisting(String uid);

	UserGroupModel getUserGroupForUID(String groupId);

	UserModel getCurrentUser();

	void setCurrentUser(UserModel user);

	CustomerModel getAnonymousUser();

	boolean isAnonymousUser(UserModel user);

	boolean isMemberOfGroup(UserModel member, UserGroupModel group);

	/** パスワードが合っているか。疑似プラットフォームは {noop} の形(平文)だけを扱う。 */
	boolean checkPassword(UserModel user, String plainPassword);

	void setPassword(UserModel user, String plainPassword);
}
