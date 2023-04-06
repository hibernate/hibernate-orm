/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.loader.ast.spi;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.service.Service;

/**
 * Factory for {@link BatchLoader} instances
 *
 * @author Steve Ebersole
 */
public interface BatchLoaderFactory extends Service {
	/**
	 * Creates a BatchLoader for batch-loadable entities
	 *
	 * @param domainBatchSize The total number of entities (max) that will be need to be initialized
	 * @param entityDescriptor The entity mapping metadata
	 */
	<T> EntityBatchLoader<T> createEntityBatchLoader(
			int domainBatchSize,
			EntityMappingType entityDescriptor,
			SessionFactoryImplementor factory);

	/**
	 * Creates a BatchLoader for batch-loadable collections.  Unlike an {@linkplain EntityBatchLoader EntityBatchLoader}, a
	 * {@code CollectionBatchLoader} is a single-use loader.
	 *
	 * todo (batch-fetch) : we could cache the collection batch loader if we know that
	 * 		1. there are no filters defined for the collection (diff from enabled)
	 * 		2. there are no fetch-profiles for the collection
	 * 	In other words, any time {@link LoadQueryInfluencers} will not affect the query
	 * 	for a given collection
	 *
	 * @param domainBatchSize The total number of collections (max) that will be initialized for any {@link CollectionBatchLoader#load}
	 * @param influencers Any load query influencers (filters, fetch-profiles, ...) in effect
	 * @param attributeMapping The collection mapping metadata
	 */
	CollectionBatchLoader createCollectionBatchLoader(
			int domainBatchSize,
			LoadQueryInfluencers influencers,
			PluralAttributeMapping attributeMapping,
			SessionFactoryImplementor factory);
}
