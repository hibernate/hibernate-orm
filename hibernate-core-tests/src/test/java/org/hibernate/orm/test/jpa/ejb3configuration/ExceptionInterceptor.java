package org.hibernate.orm.test.jpa.ejb3configuration;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.CallbackException;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;

/**
 * @author Emmanuel Bernard
 */
public class ExceptionInterceptor implements Interceptor {
	public static final String EXCEPTION_MESSAGE = "Interceptor enabled";
	protected boolean allowSave = false;

	public ExceptionInterceptor() {
		this.allowSave = false;
	}

	public ExceptionInterceptor(boolean allowSave) {
		this.allowSave = allowSave;
	}

	@Override
	public boolean onLoad(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull Object[] state,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types)
			throws CallbackException {
		throw new IllegalStateException( EXCEPTION_MESSAGE );
	}

	@Override
	public boolean onPersist(
			@Nonnull Object entity,
			@Nullable Object id,
			@Nonnull Object[] state,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types)
			throws CallbackException {
		if (allowSave) return false;
		throw new IllegalStateException( EXCEPTION_MESSAGE );
	}
}
