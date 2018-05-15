/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Additional contract for types which may be used to version (and optimistic lock) data.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface VersionSupport<T> {
	// todo (6.0) : add a Comparator here?  This would be useful for byte[] (TSQL ROWVERSION types) based versions.  But how would we inject the right Comparator?

	/**
	 * Generate an initial version.
	 *
	 * @param session The session from which this request originates.
	 * @return an instance of the type
	 */
	T seed(SharedSessionContractImplementor session);

	/**
	 * Increment the version.
	 *
	 * @param session The session from which this request originates.
	 * @param current the current version
	 * @return an instance of the type
	 */
	T next(T current, SharedSessionContractImplementor session);

	/**
	 * Generate a representation of the value for logging purposes.
	 *
	 * @param value The value to be logged
	 *
	 * @return The loggable representation
	 *
	 * @throws HibernateException An error from Hibernate
	 */
	String toLoggableString(Object value);

	/**
	 * Compare two version generated values
	 * <p/>
	 *
	 * @param x The first value
	 * @param y The second value
	 *
	 * @return True if there are considered equal
	 *
	 * @throws HibernateException A problem occurred performing the comparison
	 */
	boolean isEqual(T x, T y) throws HibernateException;

}
