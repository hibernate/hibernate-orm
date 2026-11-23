package org.hibernate.orm.test.interceptor;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Calendar;

import org.hibernate.Interceptor;
import org.hibernate.type.Type;

public class PropertyInterceptor implements Interceptor {

	@Override
	public boolean onFlushDirty(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull Object[] currentState,
			@Nullable Object[] previousState,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types) {
		currentState[1] = Calendar.getInstance();
		return true;
	}

	@Override
	public boolean onPersist(
			@Nonnull Object entity,
			@Nullable Object id,
			@Nonnull Object[] state,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types) {
		state[2] = Calendar.getInstance();
		return true;
	}

}
