/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.manytoone;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.domain.gambit.EntityWithManyToOneSelfReference;
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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				EntityWithManyToOneSelfReference.class
		}
)
@ServiceRegistry
@SessionFactory(generateStatistics = true, useCollectingStatementInspector = true)
public class EntityWithManyToOneSelfReferenceTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		final EntityWithManyToOneSelfReference entity1 = new EntityWithManyToOneSelfReference(
				1,
				"first",
				Integer.MAX_VALUE
		);

		final EntityWithManyToOneSelfReference entity2 = new EntityWithManyToOneSelfReference(
				2,
				"second",
				Integer.MAX_VALUE,
				entity1
		);

		scope.inTransaction( session -> {
			session.persist( entity1 );
			session.persist( entity2 );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testHqlSelectImplicitJoin(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithManyToOneSelfReference queryResult = session.createQuery(
							"select e from EntityWithManyToOneSelfReference e where e.other.name = 'first'",
							EntityWithManyToOneSelfReference.class
					).uniqueResult();

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
					assertThat( queryResult.getName(), is( "second" ) );

					EntityWithManyToOneSelfReference other = queryResult.getOther();
					assertTrue( Hibernate.isInitialized( other ) );
					assertThat( other.getName(), is( "first" ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
				}
		);
	}

	@Test
	public void testGetEntity(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					final EntityWithManyToOneSelfReference loaded = session.get(
							EntityWithManyToOneSelfReference.class,
							2
					);

					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );

					assertThat( loaded, notNullValue() );
					assertThat( loaded.getName(), is( "second" ) );

					EntityWithManyToOneSelfReference other = loaded.getOther();
					assertTrue( Hibernate.isInitialized( other ) );
					assertThat( other.getName(), is( "first" ) );

					statementInspector.assertExecutedCount( 1 );
				}
		);

		statementInspector.clear();
		scope.inTransaction(
				session -> {
					final EntityWithManyToOneSelfReference loaded = session.get(
							EntityWithManyToOneSelfReference.class,
							1
					);
					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );

					assertThat( loaded, notNullValue() );
					assertThat( loaded.getName(), is( "first" ) );

					EntityWithManyToOneSelfReference other = loaded.getOther();
					assertTrue( Hibernate.isInitialized( other ) );
					assertThat( other, nullValue() );
				}
		);
	}

	@Test
	public void testHqlSelectField(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithManyToOneSelfReference e where e.other.name = 'first'",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "second" ) );
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
				}
		);
	}

	@Test
	public void testHqlSelectWithJoin(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithManyToOneSelfReference result = session.createQuery(
							"select e from EntityWithManyToOneSelfReference e join e.other o where o.name = 'first'",
							EntityWithManyToOneSelfReference.class
					).uniqueResult();
					assertThat( result.getName(), equalTo( "second" ) );
					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

					assertTrue( Hibernate.isInitialized( result.getOther() ) );
				}
		);
	}

	@Test
	public void testHqlSelectWithFetchJoin(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithManyToOneSelfReference result = session.createQuery(
							"select e from EntityWithManyToOneSelfReference e join fetch e.other k where k.name = 'first'",
							EntityWithManyToOneSelfReference.class
					).uniqueResult();
					assertThat( result.getName(), equalTo( "second" ) );
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

					assertTrue( Hibernate.isInitialized( result.getOther() ) );
				}
		);
	}

	@Test
	public void testGetByMultipleIds(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					final List<EntityWithManyToOneSelfReference> list = session.byMultipleIds(
							EntityWithManyToOneSelfReference.class )
							.multiLoad( 1, 3 );
					assertThat( list.size(), equalTo( 2 ) );
					final EntityWithManyToOneSelfReference loaded = list.get( 0 );
					assertThat( loaded, is(notNullValue()) );
					assertThat( loaded.getName(), equalTo( "first" ) );
					assertThat( list.get( 1 ), is(nullValue()) );
				}
		);

		scope.inTransaction(
				session -> {
					final List<EntityWithManyToOneSelfReference> list = session.byMultipleIds(
							EntityWithManyToOneSelfReference.class )
							.multiLoad( 2, 3 );
					assertThat( list.size(), equalTo( 2 ) );
					final EntityWithManyToOneSelfReference loaded = list.get( 0 );
					assertThat( loaded, is(notNullValue()) );
					assertThat( loaded.getName(), equalTo( "second" ) );
					assertThat( loaded.getOther(), is(notNullValue()) );
					assertThat( loaded.getOther().getName(), equalTo( "first" ) );
					assertThat( list.get( 1 ), is(nullValue()) );
				}
		);
	}

	@Test
	public void testGetByMultipleIdsUnordered(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					final List<EntityWithManyToOneSelfReference> list = session.byMultipleIds(
									EntityWithManyToOneSelfReference.class )
							.enableOrderedReturn( false )
							.multiLoad( 1, 3 );
					assertThat( list.size(), equalTo( 1 ) );
					final EntityWithManyToOneSelfReference loaded = list.get( 0 );
					assertThat( loaded, is(notNullValue()) );
					assertThat( loaded.getName(), equalTo( "first" ) );
				}
		);

		scope.inTransaction(
				session -> {
					final List<EntityWithManyToOneSelfReference> list = session.byMultipleIds(
									EntityWithManyToOneSelfReference.class )
							.enableOrderedReturn( false )
							.multiLoad( 2, 3 );
					assertThat( list.size(), equalTo( 1 ) );
					final EntityWithManyToOneSelfReference loaded = list.get( 0 );
					assertThat( loaded, is(notNullValue()) );
					assertThat( loaded.getName(), equalTo( "second" ) );
					assertThat( loaded.getOther(), is(notNullValue()) );
					assertThat( loaded.getOther().getName(), equalTo( "first" ) );
				}
		);
	}
}
