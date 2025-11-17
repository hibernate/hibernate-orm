/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.manytoone;

import java.util.Calendar;

import org.hibernate.Hibernate;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.domain.gambit.EntityWithManyToOneJoinTable;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EntityWithManyToOneJoinTable.class,
				SimpleEntity.class,
				BasicEntity.class
		}
)
@ServiceRegistry
@SessionFactory(generateStatistics = true)
public class EntityWithManyToOneJoinTableTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		EntityWithManyToOneJoinTable entity = new EntityWithManyToOneJoinTable(
				1,
				"first",
				Integer.MAX_VALUE
		);

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
			session.persist( entity );
			session.persist( other );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSaveInDifferentTransactions(SessionFactoryScope scope) {
		EntityWithManyToOneJoinTable entity = new EntityWithManyToOneJoinTable( 3, "second", Integer.MAX_VALUE );

		SimpleEntity other = new SimpleEntity(
				4,
				Calendar.getInstance().getTime(),
				Calendar.getInstance().toInstant(),
				Integer.MAX_VALUE - 1,
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
					final EntityWithManyToOneJoinTable loaded = session.get( EntityWithManyToOneJoinTable.class, 1 );
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "first" ) );
					assert loaded.getOther() != null;
					assertThat( loaded.getOther().getId(), equalTo( 2 ) );
				}
		);
	}

	@Test
	public void testHqlSelect(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithManyToOneJoinTable result = session.createQuery(
							"select e from EntityWithManyToOneJoinTable e where e.id = 1",
							EntityWithManyToOneJoinTable.class
					).uniqueResult();

					assertThat( result, notNullValue() );
					assertThat( result.getId(), is( 1 ) );
					assertThat( result.getName(), is( "first" ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

					SimpleEntity other = result.getOther();
					assertTrue( Hibernate.isInitialized( other ) );
					assertThat( other.getSomeInteger(), is( Integer.MAX_VALUE ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

					// it is null so able to initialize
					assertTrue( Hibernate.isInitialized( result.getLazyOther() ) );

				}
		);

		scope.inTransaction(
				session -> {
					final EntityWithManyToOneJoinTable result = session.createQuery(
							"select e from EntityWithManyToOneJoinTable e where e.id = 1",
							EntityWithManyToOneJoinTable.class
					).uniqueResult();

					BasicEntity basicEntity = new BasicEntity( 5, "basic" );

					result.setLazyOther( basicEntity );

					session.persist( basicEntity );
				}
		);

		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithManyToOneJoinTable result = session.createQuery(
							"select e from EntityWithManyToOneJoinTable e where e.id = 1",
							EntityWithManyToOneJoinTable.class
					).uniqueResult();

					assertThat( result, notNullValue() );
					assertThat( result.getId(), is( 1 ) );
					assertThat( result.getName(), is( "first" ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

					SimpleEntity other = result.getOther();
					assertTrue( Hibernate.isInitialized( other ) );
					assertThat( other.getSomeInteger(), is( Integer.MAX_VALUE ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

					BasicEntity lazyOther = result.getLazyOther();
					assertFalse( Hibernate.isInitialized( lazyOther ) );
					assertThat( lazyOther.getId(), is( 5 ) );
					assertThat( lazyOther.getData(), is( "basic" ) );

					assertThat( statistics.getPrepareStatementCount(), is( 3L ) );
				}
		);
	}

	@Test
	public void testHqlSelectAField(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithManyToOneJoinTable e where e.other.id = 2",
							String.class
					).uniqueResult();

					assertThat( value, equalTo( "first" ) );

					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
				}
		);
	}

	@Test
	public void testHqlSelectWithJoin(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithManyToOneJoinTable result = session.createQuery(
							"select e from EntityWithManyToOneJoinTable e join e.other where e.id = 1",
							EntityWithManyToOneJoinTable.class
					).uniqueResult();
					assertThat( result, notNullValue() );
					assertThat( result.getId(), is( 1 ) );
					assertThat( result.getName(), is( "first" ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

					assertThat( result.getOther().getId(), is( 2 ) );
					assertThat( result.getOther().getSomeInteger(), is( Integer.MAX_VALUE ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
				}
		);
	}

	@Test
	public void testHqlSelectWithJoinFetch(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithManyToOneJoinTable result = session.createQuery(
							"select e from EntityWithManyToOneJoinTable e join fetch e.other where e.id = 1",
							EntityWithManyToOneJoinTable.class
					).uniqueResult();

					assertThat( result, notNullValue() );
					assertThat( result.getId(), is( 1 ) );
					assertThat( result.getName(), is( "first" ) );

					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

					assertThat( result.getOther().getId(), is( 2 ) );
					assertThat( result.getOther().getSomeInteger(), is( Integer.MAX_VALUE ) );

					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
				}
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope){
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					EntityWithManyToOneJoinTable result = session.get(
							EntityWithManyToOneJoinTable.class,
							1
					);
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
					assertThat( result, notNullValue() );
					assertThat( result.getId(), is( 1 ) );

					assertTrue( Hibernate.isInitialized( result.getOther()) );
					assertTrue( Hibernate.isInitialized( result.getLazyOther()) );
				}
		);

		scope.inTransaction(
				session -> {
					final EntityWithManyToOneJoinTable result = session.createQuery(
							"select e from EntityWithManyToOneJoinTable e where e.id = 1",
							EntityWithManyToOneJoinTable.class
					).uniqueResult();

					BasicEntity basicEntity = new BasicEntity( 5, "basic" );

					result.setLazyOther( basicEntity );

					session.persist( basicEntity );
				}
		);

		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithManyToOneJoinTable result = session.createQuery(
							"select e from EntityWithManyToOneJoinTable e where e.id = 1",
							EntityWithManyToOneJoinTable.class
					).uniqueResult();

					assertThat( result, notNullValue() );
					assertThat( result.getId(), is( 1 ) );
					assertThat( result.getName(), is( "first" ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

					SimpleEntity other = result.getOther();
					assertTrue( Hibernate.isInitialized( other ) );
					assertThat( other.getSomeInteger(), is( Integer.MAX_VALUE ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

					BasicEntity lazyOther = result.getLazyOther();
					assertFalse( Hibernate.isInitialized( lazyOther ) );
					assertThat( lazyOther.getId(), is( 5 ) );
					assertThat( lazyOther.getData(), is( "basic" ) );

					assertThat( statistics.getPrepareStatementCount(), is( 3L ) );
				}
		);
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		EntityWithManyToOneJoinTable entity = new EntityWithManyToOneJoinTable( 2, "second", Integer.MAX_VALUE );

		SimpleEntity other = new SimpleEntity(
				4,
				Calendar.getInstance().getTime(),
				null,
				100,
				Long.MAX_VALUE,
				null
		);

		entity.setOther( other );

		scope.inTransaction( session -> {
			session.persist( other );
			session.persist( entity );
		} );

		SimpleEntity anOther = new SimpleEntity(
				5,
				Calendar.getInstance().getTime(),
				null,
				Integer.MIN_VALUE + 5,
				Long.MIN_VALUE,
				null
		);

		scope.inTransaction(
				session -> {
					final EntityWithManyToOneJoinTable loaded = session.get( EntityWithManyToOneJoinTable.class, 2 );
					assert loaded != null;
					session.persist( anOther );
					loaded.setOther( anOther );
				}
		);

		scope.inTransaction(
				session -> {
					final EntityWithManyToOneJoinTable loaded = session.get( EntityWithManyToOneJoinTable.class, 2 );

					assertThat( loaded.getOther(), notNullValue() );
					assertThat( loaded.getOther().getId(), equalTo( 5 ) );
				}
		);
	}
}
