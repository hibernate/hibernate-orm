/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec;

import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.domain.gambit.EntityWithNotAggregateId;
import org.hibernate.testing.orm.domain.gambit.EntityWithNotAggregateId.PK;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

@DomainModel(
		annotatedClasses = {
				EntityWithNotAggregateId.class
		}
)
@ServiceRegistry
@SessionFactory(generateStatistics = true)
public class EntityWithNotAggregateIdTest {

	private PK entityId;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		final EntityWithNotAggregateId entity = new EntityWithNotAggregateId();
		entityId = new PK( 25, "Acme" );
		scope.inTransaction(
				session -> {
					entity.setId( entityId );
					entity.setData( "test" );
					session.persist( entity );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testHqlSelectAField(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final String value = session.createQuery( "select e.data FROM EntityWithNotAggregateId e", String.class )
							.uniqueResult();
					assertThat( value, is( "test" ) );
				}
		);
		assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
	}

	@Test
	public void testHqlSelect(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithNotAggregateId loaded = session.createQuery(
							"select e FROM EntityWithNotAggregateId e",
							EntityWithNotAggregateId.class
					).uniqueResult();
					assertThat( loaded.getData(), is( "test" ) );
					assertThat( loaded.getId(), equalTo( entityId ) );
				}
		);
		assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
	}

	@Test
	public void testHqlSelectOnlyTheEmbeddedId(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithNotAggregateId.PK value = session.createQuery(
							"select e.id FROM EntityWithNotAggregateId e",
							EntityWithNotAggregateId.PK.class
					).uniqueResult();
					assertThat( value, equalTo( entityId ) );
				}
		);
		assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithNotAggregateId loaded = session.get( EntityWithNotAggregateId.class, entityId );
					assertThat( loaded, notNullValue() );
					assertThat( loaded.getId(), notNullValue() );
					assertThat( loaded.getId(), equalTo( entityId ) );
					assertThat( loaded.getData(), is( "test" ) );
				}
		);
		assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
	}
}
