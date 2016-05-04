/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$
package org.hibernate.jpa.test.ejb3configuration;

import java.io.Serializable;

import org.hibernate.CallbackException;
import org.hibernate.type.Type;

/**
 * @author Emmanuel Bernard
 */
public class LocalExceptionInterceptor extends ExceptionInterceptor {
	public static final String LOCAL_EXCEPTION_MESSAGE = "Session-scoped interceptor enabled";

	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types)
			throws CallbackException {
		if (allowSave) return false;
		throw new IllegalStateException( LOCAL_EXCEPTION_MESSAGE );
	}
}
