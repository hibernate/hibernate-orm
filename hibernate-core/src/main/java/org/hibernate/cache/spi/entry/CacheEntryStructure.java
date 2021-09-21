/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi.entry;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Strategy for how cache entries are "structured" for storing into the cache.
 *
 * @author Gavin King
 */
public interface CacheEntryStructure {
	/**
	 * Convert the cache item into its "structured" form.  Perfectly valid to return the item as-is.
	 *
	 * @param item The item to structure.
	 *
	 * @return The structured form.
	 */
	public Object structure(Object item);

	/**
	 * Convert the previous structured form of the item back into its item form.
	 *
	 * @param structured The structured form.
	 * @param factory The session factory.
	 *
	 * @return The item
	 */
	public Object destructure(Object structured, SessionFactoryImplementor factory);
}
