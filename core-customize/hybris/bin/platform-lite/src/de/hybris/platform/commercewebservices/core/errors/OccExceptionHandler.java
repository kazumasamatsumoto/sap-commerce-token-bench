package de.hybris.platform.commercewebservices.core.errors;

import de.hybris.platform.cms2.exceptions.CMSItemNotFoundException;
import de.hybris.platform.commercefacades.order.CartNotFoundException;
import de.hybris.platform.commerceservices.customer.DuplicateUidException;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commercewebservices.core.exceptions.InvalidResourceException;
import de.hybris.platform.commercewebservices.core.exceptions.RequestParameterException;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.servicelayer.dto.converter.ConversionException;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.webservicescommons.dto.error.ErrorListWsDTO;
import de.hybris.platform.webservicescommons.dto.error.ErrorWsDTO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** OCC のエラーを本物と同じ形 {"errors":[{"type":"...Error","message":"..."}]} で返す。 */
@RestControllerAdvice
public class OccExceptionHandler
{
	private static final Logger LOG = LoggerFactory.getLogger(OccExceptionHandler.class);

	@ExceptionHandler({ UnknownIdentifierException.class, ModelNotFoundException.class })
	public ResponseEntity<ErrorListWsDTO> unknown(final RuntimeException e)
	{
		return error(HttpStatus.BAD_REQUEST, "UnknownIdentifierError", e.getMessage(), null, null);
	}

	@ExceptionHandler(InvalidResourceException.class)
	public ResponseEntity<ErrorListWsDTO> invalidResource(final InvalidResourceException e)
	{
		return error(HttpStatus.BAD_REQUEST, "InvalidResourceError", e.getMessage(), e.getResource(), "UnknownResourceError");
	}

	@ExceptionHandler(CartNotFoundException.class)
	public ResponseEntity<ErrorListWsDTO> cartNotFound(final CartNotFoundException e)
	{
		return error(HttpStatus.BAD_REQUEST, "CartError", e.getMessage(), null, "notFound");
	}

	@ExceptionHandler(CommerceCartModificationException.class)
	public ResponseEntity<ErrorListWsDTO> cartModification(final CommerceCartModificationException e)
	{
		return error(HttpStatus.BAD_REQUEST, "CartError", e.getMessage(), null, "invalid");
	}

	@ExceptionHandler(InvalidCartException.class)
	public ResponseEntity<ErrorListWsDTO> invalidCart(final InvalidCartException e)
	{
		return error(HttpStatus.BAD_REQUEST, "InvalidCartError", e.getMessage(), null, null);
	}

	@ExceptionHandler(DuplicateUidException.class)
	public ResponseEntity<ErrorListWsDTO> duplicateUid(final DuplicateUidException e)
	{
		return error(HttpStatus.BAD_REQUEST, "DuplicateUidError", e.getMessage(), null, null);
	}

	@ExceptionHandler(CMSItemNotFoundException.class)
	public ResponseEntity<ErrorListWsDTO> cmsNotFound(final CMSItemNotFoundException e)
	{
		return error(HttpStatus.BAD_REQUEST, "CMSItemNotFoundError", e.getMessage(), null, null);
	}

	@ExceptionHandler({ RequestParameterException.class, ConversionException.class, IllegalArgumentException.class,
			MethodArgumentTypeMismatchException.class })
	public ResponseEntity<ErrorListWsDTO> validation(final Exception e)
	{
		return error(HttpStatus.BAD_REQUEST, "ValidationError", e.getMessage(),
				e instanceof RequestParameterException r ? r.getSubject() : null, null);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorListWsDTO> missingParameter(final MissingServletRequestParameterException e)
	{
		return error(HttpStatus.BAD_REQUEST, "MissingServletRequestParameterError", e.getMessage(), e.getParameterName(), null);
	}

	@ExceptionHandler({ AccessDeniedException.class, AuthenticationException.class })
	public ResponseEntity<ErrorListWsDTO> accessDenied(final Exception e)
	{
		final var auth = SecurityContextHolder.getContext().getAuthentication();
		if (e instanceof AuthenticationException || auth == null || auth instanceof AnonymousAuthenticationToken)
		{
			return error(HttpStatus.UNAUTHORIZED, "UnauthorizedError", "Full authentication is required to access this resource", null, null);
		}
		return error(HttpStatus.FORBIDDEN, "ForbiddenError", "Access is denied", null, null);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorListWsDTO> method(final HttpRequestMethodNotSupportedException e)
	{
		return error(HttpStatus.METHOD_NOT_ALLOWED, "HttpRequestMethodNotSupportedError", e.getMessage(), null, null);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorListWsDTO> notFound(final NoResourceFoundException e)
	{
		return error(HttpStatus.NOT_FOUND, "NotFoundError", "There is no resource for path " + e.getResourcePath(), null, null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorListWsDTO> other(final Exception e)
	{
		LOG.error("OCC request failed", e);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, e.getClass().getSimpleName().replace("Exception", "Error"), e.getMessage(), null, null);
	}

	private static ResponseEntity<ErrorListWsDTO> error(final HttpStatus status, final String type, final String message, final String subject,
			final String reason)
	{
		final ErrorWsDTO e = new ErrorWsDTO();
		e.setType(type);
		e.setMessage(message);
		e.setSubject(subject);
		e.setReason(reason);
		final ErrorListWsDTO list = new ErrorListWsDTO();
		list.setErrors(List.of(e));
		return ResponseEntity.status(status).body(list);
	}
}
