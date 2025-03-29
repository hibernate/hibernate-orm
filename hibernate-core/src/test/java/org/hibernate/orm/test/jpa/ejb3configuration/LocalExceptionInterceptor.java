/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ejb3configuration;


import org.hibernate.CallbackException;
import org.hibernate.type.Type;

/**
 * @author Emmanuel Bernard
 */
public class LocalExceptionInterceptor extends ExceptionInterceptor {
	public static final String LOCAL_EXCEPTION_MESSAGE = "Session-scoped interceptor enabled";

	@Override
	public boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types)
			throws CallbackException {
		if (allowSave) return false;
		throw new IllegalStateException( LOCAL_EXCEPTION_MESSAGE );
	}
}
