package org.hibernate.cache.spi;

import jakarta.annotation.Nonnull;

/**
 * @author Steve Ebersole
 */
public class StandardCacheTransactionSynchronization extends AbstractCacheTransactionSynchronization {
	public StandardCacheTransactionSynchronization(@Nonnull RegionFactory regionFactory) {
		super( regionFactory );
	}
}
