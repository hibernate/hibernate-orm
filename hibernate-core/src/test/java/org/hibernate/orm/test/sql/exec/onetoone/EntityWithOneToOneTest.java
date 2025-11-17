/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.onetoone;

import java.util.Calendar;

import org.hibernate.Hibernate;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.domain.gambit.EntityWithOneToOne;
import org.hibernate.testing.orm.domain.gambit.SimpleEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hamcrest.CoreMatchers;

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
				EntityWithOneToOne.class,
				SimpleEntity.class
		}
)
@ServiceRegistry
@SessionFactory(generateStatistics = true)
public class EntityWithOneToOneTest {

	final int maxInt = Integer.MAX_VALUE - 1;
	final int minInt = Integer.MIN_VALUE + 1;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		EntityWithOneToOne entity = new EntityWithOneToOne( 1, "first", maxInt );

		SimpleEntity other = new SimpleEntity(
				2,
				Calendar.getInstance().getTime(),
				null,
				maxInt,
				Long.MAX_VALUE,
				null
		);

		entity.setOther( other );

		scope.inTransaction( session -> {
			session.persist( other );
			session.persist( entity );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		deleteAll( scope );
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithOneToOne loaded = session.get( EntityWithOneToOne.class, 1 );
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
					assertThat( loaded.getSomeInteger(), equalTo( maxInt ) );
				}
		);
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		SimpleEntity other = new SimpleEntity(
				3,
				Calendar.getInstance().getTime(),
				null,
				minInt,
				Long.MIN_VALUE,
				null
		);

		scope.inTransaction(
				session -> {
					final EntityWithOneToOne loaded = session.get( EntityWithOneToOne.class, 1 );
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "first" ) );
					assert loaded.getOther() != null;
					assertThat( loaded.getOther().getId(), equalTo( 2 ) );
					session.remove( loaded.getOther() );
					loaded.setOther( other );
					session.persist( other );
				}
		);

		scope.inTransaction(
				session -> {
					final EntityWithOneToOne loaded = session.get( EntityWithOneToOne.class, 1 );
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "first" ) );
					assert loaded.getOther() != null;
					assertThat( loaded.getOther().getId(), equalTo( 3 ) );
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
							"select e.name from EntityWithOneToOne e where e.id = 1",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "first" ) );

					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
				}
		);
	}

	@Test
	public void testHqlSelect(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithOneToOne value = session.createQuery(
							"select e from EntityWithOneToOne e where e.id = 1",
							EntityWithOneToOne.class
					).uniqueResult();
					assertThat( value.getName(), equalTo( "first" ) );

					SimpleEntity other = value.getOther();
					assertTrue( Hibernate.isInitialized( other ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

				}
		);
	}

	@Test
	public void testHqlSelectParentParentAttributeWithImplicitJoin(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithOneToOne e where e.other.id = 2",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "first" ) );

					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
				}
		);
	}

	@Test
	public void testHqlSelectWithImplicitJoin(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithOneToOne value = session.createQuery(
							"select e from EntityWithOneToOne e where e.other.id = 2",
							EntityWithOneToOne.class
					).uniqueResult();
					assertThat( value.getName(), equalTo( "first" ) );

					SimpleEntity other = value.getOther();
					assertTrue( Hibernate.isInitialized( other ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
				}
		);
	}

	@Test
	public void testHqlSelectWithJoin(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithOneToOne value = session.createQuery(
							"select e from EntityWithOneToOne e join e.other k where k.id = 2",
							EntityWithOneToOne.class
					).uniqueResult();
					assertThat( value.getName(), equalTo( "first" ) );

					SimpleEntity other = value.getOther();
					assertTrue( Hibernate.isInitialized( other ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
				}
		);
	}

	@Test
	public void testHqlSelectWithJoinFetch(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithOneToOne value = session.createQuery(
							"select e from EntityWithOneToOne e join fetch e.other k where k.id = 2",
							EntityWithOneToOne.class
					).uniqueResult();
					assertThat( value.getName(), equalTo( "first" ) );

					SimpleEntity other = value.getOther();
					assertTrue( Hibernate.isInitialized( other ) );

					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
				}
		);
	}

	private void deleteAll(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityWithOneToOne loaded = session.get( EntityWithOneToOne.class, 1 );
					assert loaded != null;
					assert loaded.getOther() != null;
					session.remove( loaded );
					session.remove( loaded.getOther() );
				}
		);

		scope.inTransaction(
				session -> {
					final EntityWithOneToOne notfound = session.find( EntityWithOneToOne.class, 1 );
					assertThat( notfound, CoreMatchers.nullValue() );
				}
		);
	}
}
