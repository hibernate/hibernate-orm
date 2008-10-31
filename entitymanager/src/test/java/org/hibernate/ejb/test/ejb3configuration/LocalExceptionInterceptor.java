//$
package org.hibernate.ejb.test.ejb3configuration;

import java.io.Serializable;

import org.hibernate.type.Type;
import org.hibernate.CallbackException;

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
