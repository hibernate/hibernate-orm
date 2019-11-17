/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.exec.manytoone;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.domain.gambit.EntityWithLazyManyToOneSelfReference;
import org.hibernate.testing.orm.domain.gambit.EntityWithManyToOneSelfReference;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
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
			session.save( entity1 );
			session.save( entity2 );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from EntityWithLazyManyToOneSelfReference" ).executeUpdate();
				}
		);
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
	@FailureExpected
	public void testGetByMultipleIds(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					final List<EntityWithLazyManyToOneSelfReference> list = session.byMultipleIds(
							EntityWithLazyManyToOneSelfReference.class )
							.multiLoad( 1, 3 );
					assert list.size() == 1;
					final EntityWithLazyManyToOneSelfReference loaded = list.get( 0 );
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "first" ) );
				}
		);

		scope.inTransaction(
				session -> {
					final List<EntityWithLazyManyToOneSelfReference> list = session.byMultipleIds(
							EntityWithLazyManyToOneSelfReference.class )
							.multiLoad( 2, 3 );
					assert list.size() == 1;
					final EntityWithLazyManyToOneSelfReference loaded = list.get( 0 );
					assert loaded != null;
					assertThat( loaded.getName(), equalTo( "second" ) );
					assert loaded.getOther() != null;
					assertThat( loaded.getOther().getName(), equalTo( "first" ) );
				}
		);
	}
}
