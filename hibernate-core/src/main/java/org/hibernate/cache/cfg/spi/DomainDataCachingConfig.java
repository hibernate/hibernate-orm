/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.cfg.spi;

import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.metamodel.model.domain.NavigableRole;

/**
 * Configuration for a specific type of data to be stored in the
 * region
 *
 * @author Steve Ebersole
 */
public interface DomainDataCachingConfig {
	/**
	 * The requested AccessType
	 */
	AccessType getAccessType();

	/**
	 * Is the data marked as being mutable?
	 */
	boolean isMutable();

	/**
	 * Is the data to be cached considered versioned?
	 */
	boolean isVersioned();

	/**
	 * The {@link NavigableRole} of the thing to be cached
	 */
	NavigableRole getNavigableRole();
}
