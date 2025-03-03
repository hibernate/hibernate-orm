/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Indicates an attempt was made to use a closed resource, such as
 * a closed {@link Session} or {@link SessionFactory}.
 *
 * @author Steve Ebersole
 */
public class ResourceClosedException extends HibernateException {
	/**
	 * Constructs a ResourceClosedException using the supplied message.
	 *
	 * @param message The message explaining the exception condition
	 */
	public ResourceClosedException(String message) {
		super( message );
	}
}
