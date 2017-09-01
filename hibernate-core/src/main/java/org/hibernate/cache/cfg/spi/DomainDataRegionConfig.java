/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.cfg.spi;

import java.util.List;

/**
 * Configuration for a named region for caching domain data
 *
 * @author Steve Ebersole
 */
public interface DomainDataRegionConfig {
	String getRegionName();

	/**
	 * Retrieve the list of all entity to be stored in this region
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
