package org.hibernate.cache.impl.bridge;

import java.util.Comparator;

import org.hibernate.cache.OptimisticCacheSource;
import org.hibernate.cache.CacheDataDescription;

/**
 * {@inheritDoc}
*
* @author Steve Ebersole
*/
public class OptimisticCacheSourceAdapter implements OptimisticCacheSource {
	private final CacheDataDescription dataDescription;

	public OptimisticCacheSourceAdapter(CacheDataDescription dataDescription) {
		this.dataDescription = dataDescription;
	}

	public boolean isVersioned() {
		return dataDescription.isVersioned();
	}

	public Comparator getVersionComparator() {
		return dataDescription.getVersionComparator();
	}
}
