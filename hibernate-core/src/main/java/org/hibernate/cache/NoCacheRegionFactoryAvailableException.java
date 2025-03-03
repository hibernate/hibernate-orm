/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache;

import org.hibernate.cfg.Environment;

/**
 * Indicates a condition where a second-level cache implementation was expected to be available, but
 * none was found on the classpath.
 *
 * @author Steve Ebersole
 */
public class NoCacheRegionFactoryAvailableException extends CacheException {
	private static final String MSG = String.format(
			"Second-level cache is used in the application, but property %s is not given; " +
					"please either disable second-level cache or set correct region factory using the %s setting " +
					"and make sure the second-level cache provider (hibernate-infinispan, e.g.) is available on the " +
					"classpath.",
			Environment.CACHE_REGION_FACTORY,
			Environment.CACHE_REGION_FACTORY
	);

	/**
	 * Constructs a NoCacheRegionFactoryAvailableException with a standard message.
	 */
	public NoCacheRegionFactoryAvailableException() {
		super( MSG );
	}
}
