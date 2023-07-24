/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.loader.ast.internal;

import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.ast.spi.BatchLoaderFactory;
import org.hibernate.loader.ast.spi.CollectionBatchLoader;
import org.hibernate.loader.ast.spi.EntityBatchLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.Type;

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
			int domainBatchSize, EntityMappingType entityDescriptor,
			SessionFactoryImplementor factory) {
		final Dialect dialect = factory.getJdbcServices().getDialect();

		// NOTE : don't use the EntityIdentifierMapping here because it will not be known until later
		final Type identifierType = entityDescriptor.getEntityPersister().getIdentifierType();
		final int idColumnCount = identifierType.getColumnSpan( factory );

		if ( idColumnCount == 1
				&& MultiKeyLoadHelper.supportsSqlArrayType( dialect )
				&& identifierType instanceof BasicType ) {
			// we can use a single ARRAY parameter to send all the ids
			return new EntityBatchLoaderArrayParam<>( domainBatchSize, entityDescriptor, factory );
		}

		final int optimalBatchSize = dialect
				.getBatchLoadSizingStrategy()
				.determineOptimalBatchLoadSize( idColumnCount, domainBatchSize, false );
		return new EntityBatchLoaderInPredicate<>( domainBatchSize, optimalBatchSize, entityDescriptor, factory );
	}

	@Override
	public CollectionBatchLoader createCollectionBatchLoader(
			int domainBatchSize,
			LoadQueryInfluencers influencers,
			PluralAttributeMapping attributeMapping,
			SessionFactoryImplementor factory) {
		final Dialect dialect = factory.getJdbcServices().getDialect();
		final int columnCount = attributeMapping.getKeyDescriptor().getJdbcTypeCount();
		if ( columnCount == 1 && MultiKeyLoadHelper.supportsSqlArrayType( dialect ) ) {
			// we can use a single ARRAY parameter to send all the ids
			return new CollectionBatchLoaderArrayParam( domainBatchSize, influencers, attributeMapping, factory );
		}

		return new CollectionBatchLoaderInPredicate( domainBatchSize, influencers, attributeMapping, factory );
	}
}
