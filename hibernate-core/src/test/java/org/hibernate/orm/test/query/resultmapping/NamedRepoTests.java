/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.resultmapping;


import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.results.ResultBuilderBasicValued;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.query.results.internal.ResultSetMappingImpl;
import org.hibernate.query.spi.QueryEngine;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;


import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Simple tests for SqlResultSetMapping handling and usage
 */
@DomainModel( annotatedClasses = SimpleEntityWithNamedMappings.class )
@SessionFactory
public class NamedRepoTests {

	@Test
	public void testMappingResolution(SessionFactoryScope sessionFactoryScope) {
		final QueryEngine queryEngine = sessionFactoryScope.getSessionFactory().getQueryEngine();
		final NamedObjectRepository namedObjectRepository = queryEngine.getNamedObjectRepository();
		final NamedResultSetMappingMemento mappingMemento = namedObjectRepository.getResultSetMappingMemento( "name" );

		final ResultSetMapping mapping = new ResultSetMappingImpl( "test" );

		final ResultSetMappingResolutionContext resolutionContext = new ResultSetMappingResolutionContext() {
			@Override
			public SessionFactoryImplementor getSessionFactory() {
				return sessionFactoryScope.getSessionFactory();
			}
		};

		mappingMemento.resolve( mapping, querySpace -> {
		}, resolutionContext );

		assertThat( mapping.getNumberOfResultBuilders(), is( 1 ) );
		mapping.visitResultBuilders(
				(position, builder) -> {
					assertThat( position, is( 0 ) );
					assertThat( builder, instanceOf( ResultBuilderBasicValued.class ) );
				}
		);
	}
}
