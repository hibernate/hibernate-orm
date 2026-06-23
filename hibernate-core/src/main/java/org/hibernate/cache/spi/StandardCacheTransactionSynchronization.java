/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
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
