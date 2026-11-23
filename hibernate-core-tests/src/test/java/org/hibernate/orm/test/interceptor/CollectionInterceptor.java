package org.hibernate.orm.test.interceptor;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Interceptor;
import org.hibernate.type.Type;

public class CollectionInterceptor implements Interceptor {

	public boolean onFlushDirty(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull Object[] currentState,
			@Nullable Object[] previousState,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types) {
		( (User) entity ).getActions().add("updated");
		return false;
	}

	public boolean onPersist(
			@Nonnull Object entity,
			@Nullable Object id,
			@Nonnull Object[] state,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types) {
		( (User) entity ).getActions().add("created");
		return false;
	}

}
