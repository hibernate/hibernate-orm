/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi.entry;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Unstructured CacheEntry format (used to store entities and collections).
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class UnstructuredCacheEntry implements CacheEntryStructure {
	/**
	 * Access to the singleton instance.
	 */
	public static final UnstructuredCacheEntry INSTANCE = new UnstructuredCacheEntry();

	@Override
	public Object structure(Object item) {
		return item;
	}

	@Override
	public Object destructure(Object structured, SessionFactoryImplementor factory) {
		return structured;
	}

	private UnstructuredCacheEntry() {
	}
}
