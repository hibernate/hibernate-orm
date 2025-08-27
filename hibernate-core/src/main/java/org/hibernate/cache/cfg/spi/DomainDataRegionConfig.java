/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.cfg.spi;

import java.util.List;

import org.hibernate.boot.spi.SessionFactoryOptions;

/**
 * Configuration for a named region for caching domain data.
 * A region's name is "unqualified"; i.e. it is not prefixed by
 * {@link SessionFactoryOptions#getCacheRegionPrefix()}.
 *
 * @author Steve Ebersole
 */
public interface DomainDataRegionConfig {

	/**
	 * Retrieve the unqualified name of this region.
	 */
	String getRegionName();

	/**
	 * Retrieve the list of all entity data to be stored in this region
	 */
	List<EntityDataCachingConfig> getEntityCaching();

	/**
	 * Retrieve the list of all natural-id data to be stored in this region
	 */
	List<NaturalIdDataCachingConfig> getNaturalIdCaching();

	/**
	 * Retrieve the list of all collection data to be stored in this region
	 */
	List<CollectionDataCachingConfig> getCollectionCaching();
}
