/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi;

import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.metamodel.model.domain.NavigableRole;

/**
 * A Region for cacheable domain data - entity, collection, natural-id.
 *
 * Generally speaking, this type of data has:
 *
 * 		* specific key and value wrapping that needs to be applied
 * 		* specific access patterns ({@link EntityDataAccess}, etc),
 * 			including some form of locking
 *
 * @author Steve Ebersole
 */
public interface DomainDataRegion extends Region {
	/**
	 * Build a EntityRegionAccess instance representing access to entity data
	 * stored in this cache region using the given AccessType.
	 *
	 * @apiNote Calling this method is illegal if the given entity is
	 * not cached
	 *
	 * @param rootEntityRole The root entity name for the hierarchy whose data
	 * we want to access
	 *
	 * @throws org.hibernate.cache.CacheException If the provider cannot provide the requested access
	 */
	EntityDataAccess getEntityDataAccess(NavigableRole rootEntityRole);

	/**
	 * Build a NaturalIdRegionAccess instance representing access to natural-id
	 * data stored in this cache region using the given AccessType.
	 *
	 * @apiNote Calling this method is illegal if the given entity is
	 * not cached
	 *
	 * @param rootEntityRole The NavigableRole of the root entity whose
	 * natural-id data we want to access
	 *
	 * @throws org.hibernate.cache.CacheException If the provider cannot provide the requested access
	 */
	NaturalIdDataAccess getNaturalIdDataAccess(NavigableRole rootEntityRole);

	/**
	 * Build a CollectionRegionAccess instance representing access to collection
	 * data stored in this cache region using the given AccessType.
	 *
	 * @apiNote Calling this method is illegal if the given entity is
	 * not cached
	 *
	 * @param collectionRole The NavigableRole of the collection whose data
	 * we want to access
	 *
	 * @throws org.hibernate.cache.CacheException If the provider cannot provide the requested access
	 */
	CollectionDataAccess getCollectionDataAccess(NavigableRole collectionRole);
}
