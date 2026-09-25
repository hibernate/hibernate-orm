package org.hibernate.loader.ast.spi;

import jakarta.annotation.Nonnull;

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
	 * Create a BatchLoader for batch-loadable entities.
	 *
	 * @param domainBatchSize The total number of entities (max) that will be need to be initialized
	 * @param entityDescriptor The entity mapping metadata
	 * @deprecated Use {@link #createEntityBatchLoader(int, EntityMappingType, LoadQueryInfluencers)} instead
	 */
	@Nonnull
	@Deprecated(forRemoval = true)
	default <T> EntityBatchLoader<T> createEntityBatchLoader(
			int domainBatchSize,
			@Nonnull EntityMappingType entityDescriptor,
			@Nonnull SessionFactoryImplementor factory) {
		return createEntityBatchLoader( domainBatchSize, entityDescriptor, new LoadQueryInfluencers( factory ) );
	}

	/**
	 * Create a BatchLoader for batch-loadable entities.
	 *
	 * @param domainBatchSize The total number of entities (max) that will be need to be initialized
	 * @param entityDescriptor The entity mapping metadata
	 */
	@Nonnull
	<T> EntityBatchLoader<T> createEntityBatchLoader(
			int domainBatchSize,
			@Nonnull EntityMappingType entityDescriptor,
			@Nonnull LoadQueryInfluencers loadQueryInfluencers);

	/**
	 * Create a BatchLoader for batch-loadable collections.
	 *
	 * @param domainBatchSize The total number of collections (max) that will be initialized for any {@link CollectionBatchLoader#load}
	 * @param influencers Any load query influencers (filters, fetch-profiles, ...) to apply to the SQL
	 * @param attributeMapping The collection mapping metadata
	 */
	@Nonnull
	CollectionBatchLoader createCollectionBatchLoader(
			int domainBatchSize,
			@Nonnull LoadQueryInfluencers influencers,
			@Nonnull PluralAttributeMapping attributeMapping,
			@Nonnull SessionFactoryImplementor factory);
}
