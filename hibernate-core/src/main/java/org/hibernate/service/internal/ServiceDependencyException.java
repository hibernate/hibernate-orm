/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.internal;
import org.hibernate.HibernateException;

/**
 * Indicates a problem processing service dependencies.
 *
 * @author Steve Ebersole
 */
public class ServiceDependencyException extends HibernateException {
	public ServiceDependencyException(String string, Throwable root) {
		super( string, root );
	}

	public ServiceDependencyException(String s) {
		super( s );
	}
}
