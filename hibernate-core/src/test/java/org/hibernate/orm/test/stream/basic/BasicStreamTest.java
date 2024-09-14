/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.stream.basic;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = BasicStreamTest.MyEntity.class
)
@SessionFactory
public class BasicStreamTest {

	@Test
	public void basicStreamTest(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					// mainly we want to make sure that closing the Stream releases the ScrollableResults too
					assertThat( ( (SessionImplementor) session ).getJdbcCoordinator()
										.getLogicalConnection()
										.getResourceRegistry()
										.hasRegisteredResources(), is( false ) );
					final Stream<MyEntity> stream = session.createQuery( "from MyEntity", MyEntity.class ).stream();
					try {
						stream.forEach( System.out::println );
						assertThat( session.getJdbcCoordinator()
											.getLogicalConnection()
											.getResourceRegistry()
											.hasRegisteredResources(), is( true ) );
					}
					finally {
						stream.close();
						assertThat( session.getJdbcCoordinator()
											.getLogicalConnection()
											.getResourceRegistry()
											.hasRegisteredResources(), is( false ) );
					}

				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10824")
	public void testQueryStream(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					MyEntity e = new MyEntity();
					e.id = 1;
					e.name = "Test";
					session.persist( e );
				}
		);

		scope.inSession(
				session -> {
					// Test stream query without type.
					try (Stream stream = session.createQuery( "From MyEntity" ).stream()) {
						Object result = stream.findFirst().orElse( null );
						assertTyping( MyEntity.class, result );
					}

					// Test stream query with type.
					try (final Stream<MyEntity> stream = session.createQuery( "From MyEntity", MyEntity.class )
							.stream()) {
						assertTyping( MyEntity.class, stream.findFirst().orElse( null ) );
					}

					// Test stream query using forEach
					try (Stream<MyEntity> stream = session.createQuery( "From MyEntity", MyEntity.class )
							.stream()) {
						stream.forEach( i -> {
							assertTyping( MyEntity.class, i );
						} );
					}

					try (Stream<Object[]> stream = session.createQuery( "SELECT me.id, me.name FROM MyEntity me" )
							.stream()) {
						stream.forEach( i -> {
							assertTyping( Integer.class, i[0] );
							assertTyping( String.class, i[1] );
						} );
					}
				}
		);

	}

	@Test
	@JiraKey(value = "HHH-11743")
	public void testTupleStream(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			MyEntity entity = new MyEntity();
			entity.id = 2;
			entity.name = "an entity";
			session.persist( entity );
		} );

		//test tuple stream using criteria
		scope.inTransaction( session -> {
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> criteria = cb.createTupleQuery();
			Root<MyEntity> me = criteria.from( MyEntity.class );
			criteria.multiselect( me.get( "id" ), me.get( "name" ) );
			try (Stream<Tuple> data = session.createQuery( criteria ).stream()) {
				data.forEach( tuple -> assertTyping( Tuple.class, tuple ) );
			}
		} );

		//test tuple stream using JPQL
		scope.inTransaction( session -> {
			try (Stream<Tuple> data = session.createQuery( "SELECT me.id, me.name FROM MyEntity me", Tuple.class )
					.stream()) {
				data.forEach( tuple -> assertTyping( Tuple.class, tuple ) );
			}
		} );
	}

	@Test
	public void basicStreamTestWithExplicitOnClose(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					AtomicInteger onCloseCount = new AtomicInteger();

					// mainly we want to make sure that closing the Stream releases the ScrollableResults too
					assertThat( session.getJdbcCoordinator()
										.getLogicalConnection()
										.getResourceRegistry()
										.hasRegisteredResources(), is( false ) );

					assertThat( onCloseCount.get(), equalTo( 0 ) );

					try (final Stream<MyEntity> stream = session.createQuery( "from MyEntity", MyEntity.class )
							.stream()
							.onClose(
									onCloseCount::incrementAndGet )) {


						assertThat( onCloseCount.get(), equalTo( 0 ) );

						stream.forEach( System.out::println );
						assertThat( session.getJdbcCoordinator()
											.getLogicalConnection()
											.getResourceRegistry()
											.hasRegisteredResources(), is( true ) );
					}

					assertThat( session.getJdbcCoordinator()
										.getLogicalConnection()
										.getResourceRegistry()
										.hasRegisteredResources(), is( false ) );

					assertThat( onCloseCount.get(), equalTo( 1 ) );
				}
		);
	}

	@Entity(name = "MyEntity")
	@Table(name = "MyEntity")
	public static class MyEntity {
		@Id
		public Integer id;
		public String name;
	}

}
