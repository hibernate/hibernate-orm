/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cache.spi;

import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.metamodel.model.domain.NavigableRole;

/**
 * A {@linkplain Region second-level cache region} that holds cacheable
 * domain data:
 * <ul>
 * <li>the destructured state of entity instances and collections, and
 * <li>mappings from natural id to primary key.
 * </ul>
 * <p>
 * This type of data has:
 * <ul>
 * <li>key and value wrapping that should to be applied, and
 * <li>defined policies for managing concurrent data access, possibly
 *     including some form of locking.
 * </ul>
 * <p>
 * These behaviors are defined by an instance of {@link EntityDataAccess},
 * {@link CollectionDataAccess}, or {@link NaturalIdDataAccess}).
 *
 * @author Steve Ebersole
 */
public interface DomainDataRegion extends Region {
	/**
	 * Build a {@link EntityDataAccess} instance representing access to
	 * destructured entity data stored in this cache region.
	 *
	 * @apiNote Calling this method is illegal if the given entity is
	 * not cacheable
	 *
	 * @param rootEntityRole The root entity name for the hierarchy whose
	 * data we want to access
	 *
	 * @throws org.hibernate.cache.CacheException If the provider cannot provide the requested access
	 */
	EntityDataAccess getEntityDataAccess(NavigableRole rootEntityRole);

	/**
	 * Build a {@link NaturalIdDataAccess} instance representing access to
	 * natural id mappings stored in this cache region.
	 *
	 * @apiNote Calling this method is illegal if the given natural id is
	 * not cacheable
	 *
	 * @param rootEntityRole The NavigableRole of the root entity whose
	 * natural id data we want to access
	 *
	 * @throws org.hibernate.cache.CacheException If the provider cannot provide the requested access
	 */
	NaturalIdDataAccess getNaturalIdDataAccess(NavigableRole rootEntityRole);

	/**
	 * Build a {@link CollectionDataAccess} instance representing access to
	 * destructured collection data stored in this cache region.
	 *
	 * @apiNote Calling this method is illegal if the given collection is
	 * not cacheable
	 *
	 * @param collectionRole The NavigableRole of the collection whose data
	 * we want to access
	 *
	 * @throws org.hibernate.cache.CacheException If the provider cannot provide the requested access
	 */
	CollectionDataAccess getCollectionDataAccess(NavigableRole collectionRole);
}
