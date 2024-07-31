/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: CollectionInterceptor.java 7700 2005-07-30 05:02:47Z oneovthafew $
package org.hibernate.orm.test.interceptor;

import org.hibernate.Interceptor;
import org.hibernate.type.Type;

public class CollectionInterceptor implements Interceptor {

	public boolean onFlushDirty(Object entity, Object id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
		( (User) entity ).getActions().add("updated");
		return false;
	}

	public boolean onSave(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
		( (User) entity ).getActions().add("created");
		return false;
	}

}
