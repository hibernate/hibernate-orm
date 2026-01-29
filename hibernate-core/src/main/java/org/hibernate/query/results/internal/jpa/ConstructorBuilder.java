/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.jpa;

import jakarta.persistence.sql.ConstructorMapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.results.spi.ResultBuilder;
import org.hibernate.query.results.spi.ResultBuilderInstantiationValued;
import org.hibernate.query.sqm.DynamicInstantiationNature;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.instantiation.internal.ArgumentDomainResult;
import org.hibernate.sql.results.graph.instantiation.internal.DynamicInstantiationResultImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import java.util.List;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * @author Steve Ebersole
 */
public class ConstructorBuilder<T> extends AbstractMappingElementBuilder<T> implements ResultBuilderInstantiationValued {
	private final MappingElementBuilder<?>[] argumentElementBuilders;

	public ConstructorBuilder(ConstructorMapping<T> constructorMapping, SessionFactoryImplementor sessionFactory) {
		super( constructorMapping.getAlias(), constructorMapping.targetClass(), sessionFactory );

		argumentElementBuilders = new MappingElementBuilder[constructorMapping.arguments().length];
		for ( int i = 0; i < constructorMapping.arguments().length; i++ ) {
			argumentElementBuilders[i] = JpaMappingHelper.toHibernateBuilder( constructorMapping.arguments()[i], sessionFactory );
		}
	}

	@Override
	public DomainResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState creationState) {
		return new DynamicInstantiationResultImpl<>(
				null,
				DynamicInstantiationNature.CLASS,
				javaType,
				argumentDomainResults( argumentElementBuilders, jdbcResultsMetadata, creationState )
		);
	}

	private static List<ArgumentDomainResult<?>> argumentDomainResults(
			MappingElementBuilder<?>[] argumentBuilders,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState creationState) {
		final List<ArgumentDomainResult<?>> argumentDomainResults = arrayList( argumentBuilders.length );
		for ( int i = 0; i < argumentBuilders.length; i++ ) {
			argumentDomainResults.add( new ArgumentDomainResult<>(
					argumentBuilders[i].buildResult( jdbcResultsMetadata, i, creationState )
			) );
		}
		return argumentDomainResults;
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
	}
}
