/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi.entry;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Structured CacheEntry format for persistent collections (other than Maps, see {@link StructuredMapCacheEntry}).
 *
 * @author Gavin King
 */
public class StructuredCollectionCacheEntry implements CacheEntryStructure {
	/**
	 * Access to the singleton reference.
	 */
	public static final StructuredCollectionCacheEntry INSTANCE = new StructuredCollectionCacheEntry();

	@Override
	public Object structure(Object item) {
		final CollectionCacheEntry entry = (CollectionCacheEntry) item;
		return Arrays.asList( entry.getState() );
	}

	@Override
	public Object destructure(Object structured, SessionFactoryImplementor factory) {
		final List list = (List) structured;
		return new CollectionCacheEntry( list.toArray( new Serializable[list.size()] ) );
	}

	private StructuredCollectionCacheEntry() {
	}
}
