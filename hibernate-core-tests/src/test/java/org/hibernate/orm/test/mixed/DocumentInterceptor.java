package org.hibernate.orm.test.mixed;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Calendar;

import org.hibernate.CallbackException;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public class DocumentInterceptor implements Interceptor {

	public boolean onFlushDirty(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull Object[] currentState,
			@Nullable Object[] previousState,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types) throws CallbackException {
		if ( entity instanceof Document ) {
			currentState[3] = Calendar.getInstance();
			return true;
		}
		else {
			return false;
		}
	}

	public boolean onPersist(
			@Nonnull Object entity,
			@Nullable Object id,
			@Nonnull Object[] state,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types) throws CallbackException {
		if ( entity instanceof Document ) {
			state[4] = state[3] = Calendar.getInstance();
			return true;
		}
		else {
			return false;
		}
	}

	public void onDelete(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nonnull Object[] state,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types) throws CallbackException {

	}
}
