//$Id$
package org.hibernate.test.interceptor;

import java.io.Serializable;

import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;

public class CollectionInterceptor extends EmptyInterceptor {

	public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
		( (User) entity ).getActions().add("updated");
		return false;
	}

	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
		( (User) entity ).getActions().add("created");
		return false;
	}

}
