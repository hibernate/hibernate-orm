package org.hibernate.orm.test.jpa.ejb3configuration;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;


import org.hibernate.CallbackException;
import org.hibernate.type.Type;

/**
 * @author Emmanuel Bernard
 */
public class LocalExceptionInterceptor extends ExceptionInterceptor {
	public static final String LOCAL_EXCEPTION_MESSAGE = "Session-scoped interceptor enabled";

	@Override
	public boolean onPersist(
			@Nonnull Object entity,
			@Nullable Object id,
			@Nonnull Object[] state,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types)
			throws CallbackException {
		if (allowSave) return false;
		throw new IllegalStateException( LOCAL_EXCEPTION_MESSAGE );
	}
}
