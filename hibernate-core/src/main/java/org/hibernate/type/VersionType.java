/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.util.Comparator;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * Additional contract for types which may be used to version (and optimistic lock) data.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface VersionType<T> extends Type {
	/**
	 * Generate an initial version.
	 *
	 * @param session The session from which this request originates.
	 * @return an instance of the type
	 */
	public T seed(SessionImplementor session);

	/**
	 * Increment the version.
	 *
	 * @param session The session from which this request originates.
	 * @param current the current version
	 * @return an instance of the type
	 */
	public T next(T current, SessionImplementor session);

	/**
	 * Get a comparator for version values.
	 *
	 * @return The comparator to use to compare different version values.
	 */
	public Comparator<T> getComparator();
}






