/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.cfg.spi;

import java.util.Comparator;

/**
 * Specialized DomainDataCachingConfig describing the requested
 * caching config for a particular persistent collection's data
 *
 * @author Steve Ebersole
 */
public interface CollectionDataCachingConfig extends DomainDataCachingConfig {
	/**
	 * The comparator to be used with the owning entity's version (if it has one).
	 */
	Comparator getOwnerVersionComparator();
}
