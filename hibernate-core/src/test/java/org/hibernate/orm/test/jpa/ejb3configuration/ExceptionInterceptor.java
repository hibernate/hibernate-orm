/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ejb3configuration;

import org.hibernate.CallbackException;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;

/**
 * @author Emmanuel Bernard
 */
public class ExceptionInterceptor implements Interceptor {
	public static final String EXCEPTION_MESSAGE = "Interceptor enabled";
	protected boolean allowSave = false;

	public ExceptionInterceptor() {
		this.allowSave = false;
	}

	public ExceptionInterceptor(boolean allowSave) {
		this.allowSave = allowSave;
	}

	@Override
	public boolean onLoad(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types)
			throws CallbackException {
		throw new IllegalStateException( EXCEPTION_MESSAGE );
	}

	@Override
	public boolean onPersist(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types)
			throws CallbackException {
		if (allowSave) return false;
		throw new IllegalStateException( EXCEPTION_MESSAGE );
	}
}
