/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.onetoone;

import java.util.Calendar;

import org.hibernate.Hibernate;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.domain.gambit.EntityWithOneToOneJoinTable;
import org.hibernate.testing.orm.domain.gambit.SimpleEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EntityWithOneToOneJoinTable.class,
				SimpleEntity.class
		}
)
@ServiceRegistry
@SessionFactory(generateStatistics = true)
public class EntityWithOneToOneJoinTableTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		EntityWithOneToOneJoinTable entity = new EntityWithOneToOneJoinTable( 1, "first", Integer.MAX_VALUE );

		SimpleEntity other = new SimpleEntity(
				2,
				Calendar.getInstance().getTime(),
				null,
				Integer.MAX_VALUE,
				Long.MAX_VALUE,
				null
		);

		entity.setOther( other );

		scope.inTransaction( session -> {
			session.persist( other );
		} );
		scope.inTransaction( session -> {
			session.persist( entity );
		} );

		scope.inTransaction(
				session -> {
					EntityWithOneToOneJoinTable entity2 = new EntityWithOneToOneJoinTable(
							2,
							"second",
							Integer.MAX_VALUE
					);

					SimpleEntity other2 = new SimpleEntity(
							3,
							Calendar.getInstance().getTime(),
							null,
							1,
							Long.MAX_VALUE,
							null
					);

					entity2.setOther( other2 );
					session.persist( other2 );
					session.persist( entity2 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithOneToOneJoinTable loaded = session.get( EntityWithOneToOneJoinTable.class, 1 );
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

					assertThat( loaded, notNullValue() );
					assertThat( loaded.getName(), equalTo( "first" ) );

					SimpleEntity other = loaded.getOther();
					assertTrue( Hibernate.isInitialized( other ) );

					assertThat( other, notNullValue() );
					assertThat( other.getId(), equalTo( 2 ) );

					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
				}
		);

		scope.inTransaction(
				session -> {
					final SimpleEntity loaded = session.get( SimpleEntity.class, 2 );
					assert loaded != null;
					assertThat( loaded.getSomeInteger(), equalTo( Integer.MAX_VALUE ) );
				}
		);
	}

	@Test
	public void testHqlSelectParentAttribute(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithOneToOneJoinTable e where e.other.id = 2",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "first" ) );
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
				}
		);
	}

	@Test
	public void testHqlSelectParentWithImplicitJoin(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithOneToOneJoinTable loaded = session.createQuery(
							"select e from EntityWithOneToOneJoinTable e where e.other.id = 2",
							EntityWithOneToOneJoinTable.class
					).uniqueResult();

					assertThat( loaded.getName(), equalTo( "first" ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

					assertThat( loaded, notNullValue() );
					assertThat( loaded.getName(), equalTo( "first" ) );

					SimpleEntity other = loaded.getOther();
					assertTrue( Hibernate.isInitialized( other ) );

					assertThat( other, notNullValue() );
					assertThat( other.getId(), equalTo( 2 ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
				}
		);
	}

	@Test
	public void testHqlSelectParentWithJoin(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithOneToOneJoinTable loaded = session.createQuery(
							"select e from EntityWithOneToOneJoinTable e join e.other o where e.id = 1",
							EntityWithOneToOneJoinTable.class
					).uniqueResult();

					assertThat( loaded.getName(), equalTo( "first" ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

					assertThat( loaded, notNullValue() );
					assertThat( loaded.getName(), equalTo( "first" ) );

					SimpleEntity other = loaded.getOther();
					assertTrue( Hibernate.isInitialized( other ) );

					assertThat( other, notNullValue() );
					assertThat( other.getId(), equalTo( 2 ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
				}
		);
	}

	@Test
	public void testHqlSelectParentWithJoinFetch(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithOneToOneJoinTable loaded = session.createQuery(
							"select e from EntityWithOneToOneJoinTable e join fetch e.other o where e.id = 1",
							EntityWithOneToOneJoinTable.class
					).uniqueResult();

					assertThat( loaded.getName(), equalTo( "first" ) );

					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

					assertThat( loaded, notNullValue() );
					assertThat( loaded.getName(), equalTo( "first" ) );

					SimpleEntity other = loaded.getOther();
					assertTrue( Hibernate.isInitialized( other ) );

					assertThat( other, notNullValue() );
					assertThat( other.getId(), equalTo( 2 ) );

					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
				}
		);
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		EntityWithOneToOneJoinTable entity = new EntityWithOneToOneJoinTable( 3, "first", Integer.MAX_VALUE );

		SimpleEntity other = new SimpleEntity(
				4,
				Calendar.getInstance().getTime(),
				null,
				5,
				Long.MAX_VALUE,
				null
		);

		entity.setOther( other );

		scope.inTransaction(
				session -> {
					session.persist( other );
					session.persist( entity );
				}
		);

		SimpleEntity anOther = new SimpleEntity(
				5,
				Calendar.getInstance().getTime(),
				null,
				Integer.MIN_VALUE,
				Long.MAX_VALUE,
				null
		);

		scope.inTransaction(
				session -> {
					EntityWithOneToOneJoinTable loaded = session.get( EntityWithOneToOneJoinTable.class, 3 );
					session.persist( anOther );
					loaded.setOther( anOther );
				}
		);

		scope.inTransaction(
				session -> {
					EntityWithOneToOneJoinTable loaded = session.get( EntityWithOneToOneJoinTable.class, 3 );
					SimpleEntity loadedOther = loaded.getOther();
					assertThat( loadedOther, notNullValue() );
					assertThat( loadedOther.getId(), equalTo( 5 ) );
				}
		);
	}
}
