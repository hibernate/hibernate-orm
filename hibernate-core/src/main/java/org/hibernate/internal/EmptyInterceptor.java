package org.hibernate.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;

import org.hibernate.Interceptor;
import org.hibernate.type.Type;

/**
 * An interceptor that does nothing.
 * This is an internal class and should not be used as a base to implement a custom Interceptor;
 * it overrides the default methods for sake of efficiency.
 *
 * Implementors of Interceptor don't need a base class anymore since we now have default
 * implementations of the contract defined in the interface.
 */
public final class EmptyInterceptor implements Interceptor, Serializable {

	public static final Interceptor INSTANCE = new EmptyInterceptor();

	private EmptyInterceptor() {
	}

	@Override
	public boolean onLoad(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull Object[] state,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types) {
		return false;
	}

	@Override
	public boolean onFlushDirty(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull Object[] currentState,
			@Nullable Object[] previousState,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types) {
		return false;
	}

	@Override
	public boolean onSave(
			@Nonnull Object entity,
			@Nullable Object id,
			@Nonnull Object[] state,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types) {
		return false;
	}

	@Override
	public void onDelete(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull Object[] state,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types) {
	}

	@Override
	@Nullable
	public int[] findDirty(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull Object[] currentState,
			@Nullable Object[] previousState,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types) {
		return null;
	}

	@Override
	@Nullable
	public Object getEntity(@Nonnull String entityName, @Nonnull Object id) {
		return null;
	}

}
