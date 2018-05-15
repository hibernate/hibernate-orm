/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 *
 * @author Steve Ebersole
 */
public interface Type<T> extends Serializable {

	JavaTypeDescriptor<T> getJavaTypeDescriptor();

	/**
	 * Get the Java type handled by this Hibernate mapping Type.  May return {@code null}
	 * in the case of non-basic types in dynamic domain models.
	 */
	default Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	/**
	 * Compare two instances of the class mapped by this type for
	 * persistence "equality" (equality of persistent state).
	 * <p/>
	 * This should always equate to some form of comparison of the value's internal state.  As an example, for
	 * something like a date the comparison should be based on its internal "time" state based on the specific portion
	 * it is meant to represent (timestamp, date, time).
	 *
	 * @param x The first value
	 * @param y The second value
	 *
	 * @return True if there are considered equal (see discussion above).
	 *
	 * @throws HibernateException A problem occurred performing the comparison
	 */
	boolean areEqual(T x, T y) throws HibernateException;

	/**
	 * Return a String representation of the given value for use in Hibernate logging.
	 */
	default String toLoggableString(Object value) {
		return value == null ? "<null>" : value.toString();
	}

}
