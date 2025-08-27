/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

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
import org.hibernate.type.Type;

import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.supportsSqlArrayType;

/**
 * Standard {@link BatchLoaderFactory} implementation
 *
 * @author Steve Ebersole
 */
public class StandardBatchLoaderFactory implements BatchLoaderFactory {
	@SuppressWarnings("unused")
	public StandardBatchLoaderFactory(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
	}

	@Override
	public <T> EntityBatchLoader<T> createEntityBatchLoader(
			int domainBatchSize,
			EntityMappingType entityDescriptor,
			LoadQueryInfluencers influencers) {
		final SessionFactoryImplementor factory = influencers.getSessionFactory();
		// NOTE : don't use the EntityIdentifierMapping here because it will not be known until later
		final Type identifierType = entityDescriptor.getEntityPersister().getIdentifierType();
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

	@Override
	public CollectionBatchLoader createCollectionBatchLoader(
			int domainBatchSize,
			LoadQueryInfluencers influencers,
			PluralAttributeMapping attributeMapping,
			SessionFactoryImplementor factory) {
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
