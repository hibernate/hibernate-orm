/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

/**
 * Thrown if a transient instance of an entity class is passed to
 * a {@link Session} method that expects a persistent instance,
 * or if the state of an entity instance cannot be made persistent
 * because the instance holds a reference to a transient entity.
 * <p>
 * An entity is considered <em>transient</em> if it is:
 * <ul>
 * <li>a newly-instantiated instance of an entity class which has
 *    never been {@linkplain Session#persist made persistent} in
 *    the database, or
 * <li>an entity instance previously associated with a persistence
 *     context which has been {@linkplain Session#remove removed}
 *     from the database.
 * </ul>
 *
 * @author Gavin King
 */
public class TransientObjectException extends HibernateException {
	/**
	 * Constructs a {@code TransientObjectException} using the supplied message.
	 *
	 * @param message The message explaining the exception condition
	 */
	public TransientObjectException(String message) {
		super( message );
	}

}
