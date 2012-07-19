//$Id$
package org.hibernate.jpa.test.ejb3configuration;

import java.io.Serializable;

import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;

/**
 * @author Emmanuel Bernard
 */
public class ExceptionInterceptor extends EmptyInterceptor {
	public static final String EXCEPTION_MESSAGE = "Interceptor enabled";
	protected boolean allowSave = false;

	public ExceptionInterceptor() {
		this.allowSave = false;
	}

	public ExceptionInterceptor(boolean allowSave) {
		this.allowSave = allowSave;
	}

	public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types)
			throws CallbackException {
		throw new IllegalStateException( EXCEPTION_MESSAGE );
	}

	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types)
			throws CallbackException {
		if (allowSave) return false;
		throw new IllegalStateException( EXCEPTION_MESSAGE );
	}
}
