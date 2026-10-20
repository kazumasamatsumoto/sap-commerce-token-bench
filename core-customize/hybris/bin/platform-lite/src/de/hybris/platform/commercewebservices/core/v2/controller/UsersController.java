package de.hybris.platform.commercewebservices.core.v2.controller;

import de.hybris.platform.commercefacades.customer.CustomerFacade;
import de.hybris.platform.commercefacades.user.data.CustomerData;
import de.hybris.platform.commercefacades.user.data.RegisterData;
import de.hybris.platform.commerceservices.customer.DuplicateUidException;
import de.hybris.platform.commercewebservices.core.exceptions.RequestParameterException;
import de.hybris.platform.commercewebservicescommons.dto.user.UserSignUpWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.user.UserWsDTO;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/{baseSiteId}/users")
public class UsersController extends BaseController
{
	private static final String EMAIL = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

	@Resource(name = "customerFacade")
	private CustomerFacade customerFacade;

	/** POST /{baseSiteId}/users … 会員登録。uid はメールアドレス、パスワードは 8 文字以上。 */
	@Secured({ "ROLE_CLIENT", "ROLE_TRUSTED_CLIENT", "ROLE_ANONYMOUS" })
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UserWsDTO createUser(@RequestBody final UserSignUpWsDTO user, @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
			throws DuplicateUidException
	{
		if (user.getUid() == null || !user.getUid().matches(EMAIL))
		{
			throw new RequestParameterException("uid must be a valid email address", "uid");
		}
		if (user.getPassword() == null || user.getPassword().length() < 8)
		{
			throw new RequestParameterException("password must be at least 8 characters", "password");
		}
		final RegisterData data = new RegisterData();
		data.setLogin(user.getUid());
		data.setPassword(user.getPassword());
		data.setFirstName(user.getFirstName());
		data.setLastName(user.getLastName());
		data.setTitleCode(user.getTitleCode());
		return getDataMapper().map(customerFacade.register(data), UserWsDTO.class, fields);
	}

	/** GET /{baseSiteId}/users/{userId}(userId は current か、自分の uid) */
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT" })
	@GetMapping("/{userId}")
	public UserWsDTO getUser(@PathVariable final String userId, @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		return getDataMapper().map(customerFacade.getCurrentCustomer(), UserWsDTO.class, fields);
	}

	/** PATCH /{baseSiteId}/users/{userId} … 名前と敬称を変える。 */
	@Secured({ "ROLE_CUSTOMERGROUP", "ROLE_TRUSTED_CLIENT" })
	@PatchMapping("/{userId}")
	@ResponseStatus(HttpStatus.OK)
	public void updateUser(@PathVariable final String userId, @RequestBody final UserWsDTO user)
	{
		final CustomerData data = new CustomerData();
		data.setFirstName(user.getFirstName());
		data.setLastName(user.getLastName());
		data.setTitleCode(user.getTitleCode());
		customerFacade.updateProfile(data);
	}
}
