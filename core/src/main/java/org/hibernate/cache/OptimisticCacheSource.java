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
	 * @return
	 */
	public Comparator getVersionComparator();
}
