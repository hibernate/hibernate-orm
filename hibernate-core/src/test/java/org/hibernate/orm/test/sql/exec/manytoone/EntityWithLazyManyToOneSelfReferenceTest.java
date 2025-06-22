/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.manytoone;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.domain.gambit.EntityWithLazyManyToOneSelfReference;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				EntityWithLazyManyToOneSelfReference.class
		}
)
@ServiceRegistry
@SessionFactory(generateStatistics = true)
public class EntityWithLazyManyToOneSelfReferenceTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		final EntityWithLazyManyToOneSelfReference entity1 = new EntityWithLazyManyToOneSelfReference(
				1,
				"first",
				Integer.MAX_VALUE
		);

		final EntityWithLazyManyToOneSelfReference entity2 = new EntityWithLazyManyToOneSelfReference(
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
					final EntityWithLazyManyToOneSelfReference queryResult = session.createQuery(
							"select e from EntityWithLazyManyToOneSelfReference e where e.other.name = 'first'",
							EntityWithLazyManyToOneSelfReference.class
					).uniqueResult();

					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
					assertThat( queryResult.getName(), is( "second" ) );

					EntityWithLazyManyToOneSelfReference other = queryResult.getOther();
					assertFalse( Hibernate.isInitialized( other ) );
					assertThat( other.getName(), is( "first" ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
				}
		);
	}

	@Test
	public void testGetEntity(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithLazyManyToOneSelfReference loaded = session.get(
							EntityWithLazyManyToOneSelfReference.class,
							2
					);

					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

					assertThat( loaded, notNullValue() );
					assertThat( loaded.getName(), is( "second" ) );

					EntityWithLazyManyToOneSelfReference other = loaded.getOther();
					assertFalse( Hibernate.isInitialized( other ) );
					assertThat( other.getName(), is( "first" ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
				}
		);

		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithLazyManyToOneSelfReference loaded = session.get(
							EntityWithLazyManyToOneSelfReference.class,
							1
					);
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
					assertThat( loaded, notNullValue() );
					assertThat( loaded.getName(), is( "first" ) );

					EntityWithLazyManyToOneSelfReference other = loaded.getOther();
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
							"select e.name from EntityWithLazyManyToOneSelfReference e where e.other.name = 'first'",
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
					final EntityWithLazyManyToOneSelfReference result = session.createQuery(
							"select e from EntityWithLazyManyToOneSelfReference e join e.other o where o.name = 'first'",
							EntityWithLazyManyToOneSelfReference.class
					).uniqueResult();
					assertThat( result.getName(), equalTo( "second" ) );
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

					EntityWithLazyManyToOneSelfReference other = result.getOther();
					assertFalse( Hibernate.isInitialized( other ) );
					assertThat( other.getName(), is( "first" ) );

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );				}
		);
	}

	@Test
	public void testHqlSelectWithFetchJoin(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithLazyManyToOneSelfReference result = session.createQuery(
							"select e from EntityWithLazyManyToOneSelfReference e join fetch e.other k where k.name = 'first'",
							EntityWithLazyManyToOneSelfReference.class
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
					final List<EntityWithLazyManyToOneSelfReference> list = session.byMultipleIds(
							EntityWithLazyManyToOneSelfReference.class )
							.multiLoad( 1, 3 );
					// ordered-returns (the default) returns a list with one (possibly null) element
					// per requested id.  Here we have 2 ids so the list should have 2 elements, the
					// second one (#3) being null
					org.assertj.core.api.Assertions.assertThat( list ).hasSize( 2 );

					final EntityWithLazyManyToOneSelfReference entity1 = list.get( 0 );
					org.assertj.core.api.Assertions.assertThat( entity1 ).isNotNull();
					org.assertj.core.api.Assertions.assertThat( entity1.getName() ).isEqualTo( "first" );

					final EntityWithLazyManyToOneSelfReference entity3 = list.get( 1 );
					org.assertj.core.api.Assertions.assertThat( entity3 ).isNull();
				}
		);

		scope.inTransaction(
				session -> {
					final List<EntityWithLazyManyToOneSelfReference> list = session.byMultipleIds(
							EntityWithLazyManyToOneSelfReference.class )
							.multiLoad( 2, 3 );

					// same as above, here we expect a List with 2 elements - one null and one non-null
					org.assertj.core.api.Assertions.assertThat( list ).hasSize( 2 );

					final EntityWithLazyManyToOneSelfReference entity2 = list.get( 0 );
					org.assertj.core.api.Assertions.assertThat( entity2 ).isNotNull();
					org.assertj.core.api.Assertions.assertThat( entity2.getName() ).isEqualTo( "second" );
					assertThat( entity2.getOther().getName(), equalTo( "first" ) );
				}
		);
	}
}
