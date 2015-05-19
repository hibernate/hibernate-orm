/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

import java.util.Comparator;

/**
 * Contract for sources of optimistically lockable data sent to the second level
 * cache.
 * <p/>
 * Note currently {@link org.hibernate.persister.entity.EntityPersister}s are
 * the only viable source.
 *
 * @author Steve Ebersole
 */
public interface OptimisticCacheSource {
	/**
	 * Is the data to be cached considered versioned?
	 * <p/>
	 * If true, it is illegal for {@link #getVersionComparator} to return
	 * null.
	 *
	 * @return True if the data is versioned; false otherwise.
	 */
	public boolean isVersioned();

	/**
	 * Get the comparator used to compare two different version values.
	 * <p/>
	 * May return null <b>if</b> {@link #isVersioned()} returns false.
	 * @return Comparator used to compare two different version values.
	 */
	public Comparator getVersionComparator();
}
