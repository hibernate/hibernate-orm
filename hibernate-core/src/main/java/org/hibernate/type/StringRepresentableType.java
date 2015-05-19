/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.HibernateException;

/**
 * Additional, optional contract for types capable of rendering and consuming their values to/from strings.
 *
 * @author Steve Ebersole
 */
public interface StringRepresentableType<T> {
	/**
	 * Render the value to the string representation.
	 *
	 * @param value The value to render to string.
	 *
	 * @return The string representation
	 *
	 * @throws HibernateException Problem rendering
	 */
	String toString(T value) throws HibernateException;

	/**
	 * Consume the given string representation back into this types java form.
	 *
	 * @param string The string representation to be consumed.
	 *
	 * @return The java type representation
	 *
	 * @throws HibernateException Problem consuming
	 */
	T fromStringValue(String string) throws HibernateException;
}
