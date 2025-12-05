/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 */
public class UnknownUnwrapTypeException extends HibernateException {
	public UnknownUnwrapTypeException(Class<?> unwrapType) {
		super( "Cannot unwrap to requested type [" + unwrapType.getName() + "]" );
	}

	public UnknownUnwrapTypeException(Class<?> unwrapType, Throwable root) {
		this( unwrapType );
		super.initCause( root );
	}
}
