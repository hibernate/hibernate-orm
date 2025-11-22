/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Thrown if a detached instance of an entity class is passed to
 * a {@link Session} method that expects a managed instance.
 *
 * @author Gavin King
 *
 * @since 7.0
 */
@Incubating
public class DetachedObjectException extends HibernateException {
	public DetachedObjectException(String message) {
		super( message );
	}
}
