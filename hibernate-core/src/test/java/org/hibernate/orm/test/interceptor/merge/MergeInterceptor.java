/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor.merge;

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
	public boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
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
	public void preMerge(Object entity, Object[] state, String[] propertyNames, Type[] types) {
		for ( int i = 0; i < types.length; i++ ) {
			if ( types[i].isCollectionType() && state[i] == null ) {
				state[i] = Hibernate.set().createDetachedInstance();
			}
		}
	}
}
