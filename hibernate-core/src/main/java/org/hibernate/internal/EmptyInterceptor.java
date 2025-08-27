/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

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
	public boolean onLoad(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
		return false;
	}

	@Override
	public boolean onFlushDirty(
			Object entity,
			Object id,
			Object[] currentState,
			Object[] previousState,
			String[] propertyNames,
			Type[] types) {
		return false;
	}

	@Override
	public boolean onSave(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
		return false;
	}

	@Override
	public void onDelete(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
	}

	@Override
	public int[] findDirty(
			Object entity,
			Object id,
			Object[] currentState,
			Object[] previousState,
			String[] propertyNames,
			Type[] types) {
		return null;
	}

	@Override
	public Object getEntity(String entityName, Object id) {
		return null;
	}

}
