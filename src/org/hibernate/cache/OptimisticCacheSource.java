package org.hibernate.cache;

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
	 * Does this source represent versioned (i.e., and thus optimistically
	 * lockable) data?
	 *
	 * @return True if this source represents versioned data; false otherwise.
	 */
	public boolean isVersioned();

	/**
	 * Get the comparator used to compare two different version values together.
	 *
	 * @return An appropriate comparator.
	 */
	public Comparator getVersionComparator();
}
