package org.hibernate.orm.test.interceptor.merge;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Hibernate;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;

/**
 * An interceptor that initializes null collection references
 * when an entity is passed to persist() or merge().
 *
 * @author Gavin King
 */
public class MergeInterceptor implements Interceptor {
	@Override
	public boolean onPersist(
			@Nonnull Object entity,
			@Nullable Object id,
			@Nonnull Object[] state,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types) {
		boolean result = false;
		for ( int i = 0; i < types.length; i++ ) {
			if ( types[i].isCollectionType() && state[i] == null ) {
				state[i] = Hibernate.set().createNewInstance();
				result = true;
			}
		}
		return result;
	}

	@Override
	public void preMerge(
			@Nonnull Object entity,
			@Nonnull Object[] state,
			@Nonnull String[] propertyNames,
			@Nonnull Type[] types) {
		for ( int i = 0; i < types.length; i++ ) {
			if ( types[i].isCollectionType() && state[i] == null ) {
				state[i] = Hibernate.set().createDetachedInstance();
			}
		}
	}
}
