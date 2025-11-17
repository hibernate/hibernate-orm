/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.onetoone;

import java.util.Calendar;

import org.hibernate.Hibernate;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.domain.gambit.EntityWithOneToOneSharingPrimaryKey;
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
				EntityWithOneToOneSharingPrimaryKey.class,
				SimpleEntity.class
		}
)
@ServiceRegistry
@SessionFactory(generateStatistics = true)
public class EntityWithOneToOneSharingPrimaryKeyTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		SimpleEntity other = new SimpleEntity(
				2,
				Calendar.getInstance().getTime(),
				null,
				Integer.MAX_VALUE,
				Long.MAX_VALUE,
				null
		);

		EntityWithOneToOneSharingPrimaryKey entity = new EntityWithOneToOneSharingPrimaryKey(
				other.getId(),
				"first",
				Integer.MAX_VALUE
		);

		entity.setOther( other );

		scope.inTransaction(
				session -> {
					session.persist( other );
					session.persist( entity );
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
					final EntityWithOneToOneSharingPrimaryKey loaded = session.get(
							EntityWithOneToOneSharingPrimaryKey.class,
							2
					);
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
					final SimpleEntity loaded = session.get(
							SimpleEntity.class,
							2
					);
					assertThat( loaded, notNullValue() );
				}
		);
	}

	@Test
	public void testHqlSelectWithImplicitJoin(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithOneToOneSharingPrimaryKey value = session.createQuery(
							"select e from EntityWithOneToOneSharingPrimaryKey e where e.other.id = 2",
							EntityWithOneToOneSharingPrimaryKey.class
					).uniqueResult();
					assertThat( value.getName(), equalTo( "first" ) );
					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

					assertTrue( Hibernate.isInitialized( value.getOther() ) );
				}
		);
	}

	@Test
	public void testHqlSelectWithJoin(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithOneToOneSharingPrimaryKey value = session.createQuery(
							"select e from EntityWithOneToOneSharingPrimaryKey e join e.other t where t.id = 2",
							EntityWithOneToOneSharingPrimaryKey.class
					).uniqueResult();
					assertThat( value.getName(), equalTo( "first" ) );
					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

					assertTrue( Hibernate.isInitialized( value.getOther() ) );
				}
		);
	}

	@Test
	public void testHqlSelectFetchWithJoin(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithOneToOneSharingPrimaryKey value = session.createQuery(
							"select e from EntityWithOneToOneSharingPrimaryKey e join fetch e.other t where t.id = 2",
							EntityWithOneToOneSharingPrimaryKey.class
					).uniqueResult();
					assertThat( value.getName(), equalTo( "first" ) );
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

					assertTrue( Hibernate.isInitialized( value.getOther() ) );

				}
		);
	}

	@Test
	public void testHqlSelectAField(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithOneToOneSharingPrimaryKey e where e.other.id = 2",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "first" ) );
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
				}
		);
	}
}
