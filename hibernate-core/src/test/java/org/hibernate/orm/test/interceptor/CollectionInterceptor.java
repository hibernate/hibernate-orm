/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor;

import org.hibernate.Interceptor;
import org.hibernate.type.Type;

public class CollectionInterceptor implements Interceptor {

	public boolean onFlushDirty(Object entity, Object id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
		( (User) entity ).getActions().add("updated");
		return false;
	}

	public boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
		( (User) entity ).getActions().add("created");
		return false;
	}

}
