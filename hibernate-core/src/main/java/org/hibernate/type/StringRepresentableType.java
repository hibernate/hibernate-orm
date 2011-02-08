/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
	public abstract String toString(T value) throws HibernateException;

	/**
	 * Consume the given string representation back into this types java form.
	 *
	 * @param string The string representation to be consumed.
	 *
	 * @return The java type representation
	 *
	 * @throws HibernateException Problem consuming
	 */
	public abstract T fromStringValue(String string) throws HibernateException;
}
