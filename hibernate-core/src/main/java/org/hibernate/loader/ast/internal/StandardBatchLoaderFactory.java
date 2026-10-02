package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nonnull;

import java.util.Map;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.ast.spi.BatchLoaderFactory;
import org.hibernate.loader.ast.spi.CollectionBatchLoader;
import org.hibernate.loader.ast.spi.EntityBatchLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.BasicType;

import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.supportsSqlArrayType;

/**
 * Standard {@link BatchLoaderFactory} implementation
 *
 * @author Steve Ebersole
 */
public class StandardBatchLoaderFactory implements BatchLoaderFactory {
	@SuppressWarnings("unused")
	public StandardBatchLoaderFactory(@Nonnull Map<String, Object> configurationValues, @Nonnull ServiceRegistryImplementor registry) {
	}

	@Nonnull
	@Override
	public <T> EntityBatchLoader<T> createEntityBatchLoader(
			int domainBatchSize,
			@Nonnull EntityMappingType entityDescriptor,
			@Nonnull LoadQueryInfluencers influencers) {
		final var factory = influencers.getSessionFactory();
		// NOTE: don't use the EntityIdentifierMapping here because it will not be known until later
		final var identifierType = entityDescriptor.getEntityPersister().getIdentifierType();
		if ( identifierType.getColumnSpan( factory.getRuntimeMetamodels() ) == 1
				&& supportsSqlArrayType( factory.getJdbcServices().getDialect() )
				&& identifierType instanceof BasicType ) {
			// we can use a single ARRAY parameter to send all the ids
			return new EntityBatchLoaderArrayParam<>( domainBatchSize, entityDescriptor, influencers );
		}
		else {
			return new EntityBatchLoaderInPredicate<>( domainBatchSize, entityDescriptor, influencers );
		}
	}

	@Nonnull
	@Override
	public CollectionBatchLoader createCollectionBatchLoader(
			int domainBatchSize,
			@Nonnull LoadQueryInfluencers influencers,
			@Nonnull PluralAttributeMapping attributeMapping,
			@Nonnull SessionFactoryImplementor factory) {
		if ( attributeMapping.getKeyDescriptor().getJdbcTypeCount() == 1
				&& supportsSqlArrayType( factory.getJdbcServices().getDialect() ) ) {
			// we can use a single ARRAY parameter to send all the ids
			return new CollectionBatchLoaderArrayParam( domainBatchSize, influencers, attributeMapping, factory );
		}
		else {
			return new CollectionBatchLoaderInPredicate( domainBatchSize, influencers, attributeMapping, factory );
		}
	}
}
