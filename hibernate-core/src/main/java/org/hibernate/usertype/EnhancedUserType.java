/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.usertype;

import org.hibernate.HibernateException;

/**
 * A custom type that may function as an identifier or discriminator type
 *
 * @author Gavin King
 */
public interface EnhancedUserType<J> extends UserType<J> {

	/**
	 * Return an SQL literal representation of the value
	 */
	String toSqlLiteral(J value);

	/**
	 * Render the value to the string representation.
	 *
	 * @param value The value to render to string.
	 *
	 * @return The string representation
	 *
	 * @throws HibernateException Problem rendering
	 */
	String toString(J value) throws HibernateException;

	/**
	 * Consume the given string representation back into this types java form.
	 *
	 * @param sequence The string representation to be consumed.
	 *
	 * @return The java type representation
	 *
	 * @throws HibernateException Problem consuming
	 */
	J fromStringValue(CharSequence sequence) throws HibernateException;

}
