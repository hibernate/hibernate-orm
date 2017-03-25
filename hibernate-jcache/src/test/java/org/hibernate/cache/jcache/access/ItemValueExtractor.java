/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.jcache.access;

import org.hibernate.cache.jcache.JCacheTransactionalDataRegion;


/**
 * @author Alex Snaps
 */
public class ItemValueExtractor extends AbstractReadWriteRegionAccessStrategy<JCacheTransactionalDataRegion> {


	/**
	 * Creates a read/write cache access strategy around the given cache region.
	 */
	public ItemValueExtractor(JCacheTransactionalDataRegion region) {
		super(region);
	}


	public static <T> T getValue(final Object entry) {
		if(!(entry instanceof Item)) {
			throw new IllegalArgumentException("Entry needs to be of type " + Item.class.getName());
		}
		return (T)((Item)entry).getValue();
	}
}
