/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

/**
 * @author Steve Ebersole
 */
public class StandardCacheTransactionSynchronization extends AbstractCacheTransactionSynchronization {
	public StandardCacheTransactionSynchronization(RegionFactory regionFactory) {
		super( regionFactory );
	}
}
