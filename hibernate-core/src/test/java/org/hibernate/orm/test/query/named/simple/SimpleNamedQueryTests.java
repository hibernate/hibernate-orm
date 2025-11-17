/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.named.simple;

import jakarta.persistence.FlushModeType;

import org.hibernate.CacheMode;
import org.hibernate.query.Query;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.sqm.spi.NamedSqmQueryMemento;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = SimpleEntityWithNamedQueries.class )
@SessionFactory
public class SimpleNamedQueryTests {
	@Test
	public void testBinding(SessionFactoryScope scope) {
		final NamedObjectRepository namedObjectRepository = scope.getSessionFactory()
				.getQueryEngine()
				.getNamedObjectRepository();

		final NamedSqmQueryMemento<?> simpleMemento = namedObjectRepository.getSqmQueryMemento( "simple" );
		assertThat( simpleMemento, notNullValue() );

		final NamedSqmQueryMemento<?> restrictedMemento = namedObjectRepository.getSqmQueryMemento( "restricted" );
		assertThat( restrictedMemento, notNullValue() );
	}

	@Test
	public void testExecution(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createNamedQuery( "simple" ).list();
					session.createNamedQuery( "restricted" ).setParameter( "name", "a name" ).list();
				}
		);
		final NamedObjectRepository namedObjectRepository = scope.getSessionFactory()
				.getQueryEngine()
				.getNamedObjectRepository();

		final NamedSqmQueryMemento<?> simpleMemento = namedObjectRepository.getSqmQueryMemento( "simple" );
		assertThat( simpleMemento, notNullValue() );

		final NamedSqmQueryMemento<?> restrictedMemento = namedObjectRepository.getSqmQueryMemento( "restricted" );
		assertThat( restrictedMemento, notNullValue() );
	}

	@Test
	public void testStoring(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String qryString = "select e from SimpleEntityWithNamedQueries e where e.id = :id";
					final Query<SimpleEntityWithNamedQueries> query = session.createQuery( qryString, SimpleEntityWithNamedQueries.class );
					session.getSessionFactory().addNamedQuery( "byId", query );
				}
		);

		scope.inTransaction(
				session -> {
					session.createNamedQuery( "byId" ).setParameter( "id", 1 ).list();
				}
		);
	}

	@Test
	public void testOptions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String qryString = "select e from SimpleEntityWithNamedQueries e where e.id = :id";
					final Query<SimpleEntityWithNamedQueries> query = session.createQuery( qryString, SimpleEntityWithNamedQueries.class );
					query.setFetchSize( 20 );
					query.setFirstResult( 20 );
					query.setMaxResults( 20 );
					query.setFlushMode( FlushModeType.COMMIT );
					query.setCacheMode( CacheMode.IGNORE );
					query.setCacheRegion( "custom-region" );
					session.getSessionFactory().addNamedQuery( "options", query );
				}
		);

		scope.inTransaction(
				session -> {
					final Query<SimpleEntityWithNamedQueries> query = session.createNamedQuery( "options", SimpleEntityWithNamedQueries.class );
					assertThat( query.getFetchSize(), is( 20 ) );
					assertThat( query.getFirstResult(), is( 20 ) );
					assertThat( query.getMaxResults(), is( 20 ) );
					assertThat( query.getFlushMode(), is( FlushModeType.COMMIT ) );
					assertThat( query.getCacheMode(), is( CacheMode.IGNORE ) );
					assertThat( query.getCacheRegion(), is( "custom-region" ) );
				}
		);
	}
}
