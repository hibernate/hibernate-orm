/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat;

import java.util.Set;

/**
 * Information about the first-level (session) cache
 * for a particular session instance
 * @author Gavin King
 */
public interface SessionStatistics {
	/**
	 * Get the number of entity instances associated with the session
	 */
	public int getEntityCount();
	/**
	 * Get the number of collection instances associated with the session
	 */
	public int getCollectionCount();

	/**
	 * Get the set of all <tt>EntityKey</tt>s
	 * @see org.hibernate.engine.spi.EntityKey
	 */
	public Set getEntityKeys();
	/**
	 * Get the set of all <tt>CollectionKey</tt>s
	 * @see org.hibernate.engine.spi.CollectionKey
	 */
	public Set getCollectionKeys();
	
}
