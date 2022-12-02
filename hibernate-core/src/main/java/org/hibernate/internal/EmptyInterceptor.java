/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.io.Serializable;

import org.hibernate.Interceptor;
import org.hibernate.type.Type;

/**
 * An interceptor that does nothing.
 * This is an internal class and should not be used as a base to implement a custom Interceptor;
 * it is similar to the public, deprecated {@link org.hibernate.EmptyInterceptor} but overrides
 * the default methods for sake of efficiency: this wasn't possible on the original deprecated
 * copy as that wouldn't have been backwards compatible. For this reason this copy is internal.
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
