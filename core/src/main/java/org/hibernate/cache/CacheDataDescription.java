package org.hibernate.cache;

import java.util.Comparator;

/**
 * Describes attributes regarding the type of data to be cached.
 *
 * @author Steve Ebersole
 */
public interface CacheDataDescription {
	/**
	 * Is the data marked as being mutable?
	 *
	 * @return True if the data is mutable; false otherwise.
	 */
	public boolean isMutable();

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
