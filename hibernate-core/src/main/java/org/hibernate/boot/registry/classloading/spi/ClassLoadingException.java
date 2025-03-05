/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.classloading.spi;
import org.hibernate.HibernateException;

/**
 * Indicates a problem performing class loading.
 *
 * @author Steve Ebersole
 */
public class ClassLoadingException extends HibernateException {
	/**
	 * Constructs a ClassLoadingException using the specified message and cause.
	 *
	 * @param message A message explaining the exception condition.
	 * @param cause The underlying cause
	 */
	public ClassLoadingException(String message, Throwable cause) {
		super( message, cause );
	}

	/**
	 * Constructs a ClassLoadingException using the specified message.
	 *
	 * @param message A message explaining the exception condition.
	 */
	public ClassLoadingException(String message) {
		super( message );
	}
}
