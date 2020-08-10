/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.named.resultmapping;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.named.NamedQueryRepository;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.query.results.ResultSetMappingImpl;
import org.hibernate.query.spi.QueryEngine;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = SimpleEntityWithNamedMappings.class )
@SessionFactory
public class SimpleNamedMappingTests {
	@Test
	public void testMappingResolution(SessionFactoryScope sessionFactoryScope) {
		final QueryEngine queryEngine = sessionFactoryScope.getSessionFactory().getQueryEngine();
		final NamedQueryRepository namedQueryRepository = queryEngine.getNamedQueryRepository();
		final NamedResultSetMappingMemento mappingMemento = namedQueryRepository.getResultSetMappingMemento( "name" );

		final ResultSetMapping mapping = new ResultSetMappingImpl( "test" );

		final ResultSetMappingResolutionContext resolutionContext = new ResultSetMappingResolutionContext() {
			@Override
			public SessionFactoryImplementor getSessionFactory() {
				return sessionFactoryScope.getSessionFactory();
			}
		};

		mappingMemento.resolve( mapping, querySpace -> {}, resolutionContext );

		assertThat( mapping.getNumberOfResultBuilders(), is( 1 ) );
	}
}
