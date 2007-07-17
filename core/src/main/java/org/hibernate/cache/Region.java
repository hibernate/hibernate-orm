package org.hibernate.cache;

import java.util.Map;

/**
 * Defines a contract for accessing a particular named region within the 
 * underlying cache implementation.
 *
 * @author Steve Ebersole
 */
public interface Region {
	/**
	 * Retrieve the name of this region.
	 *
	 * @return The region name
	 */
	public String getName();

	/**
	 * The "end state" contract of the region's lifecycle.  Called
	 * during {@link org.hibernate.SessionFactory#close()} to give
	 * the region a chance to cleanup.
	 *
	 * @throws CacheException Indicates problem shutting down
	 */
	public void destroy() throws CacheException;

	/**
	 * The number of bytes is this cache region currently consuming in memory.
	 *
	 * @return The number of bytes consumed by this region; -1 if unknown or
	 * unsupported.
	 */
	public long getSizeInMemory();

	/**
	 * The count of entries currently contained in the regions in-memory store.
	 *
	 * @return The count of entries in memory; -1 if unknown or unsupported.
	 */
	public long getElementCountInMemory();

	/**
	 * The count of entries currently contained in the regions disk store.
	 *
	 * @return The count of entries on disk; -1 if unknown or unsupported.
	 */
	public long getElementCountOnDisk();

	/**
	 * Get the contents of this region as a map.
	 * <p/>
	 * Implementors which do not support this notion
	 * should simply return an empty map.
	 *
	 * @return The content map.
	 */
	public Map toMap();

	public long nextTimestamp();
	public int getTimeout();
}
