/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stream.basic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.Session;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.jdbc.ResourceRegistry;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = JpaStreamTest.MyEntity.class
)
@SessionFactory
public class JpaStreamTest {

	@Test
	@JiraKey(value = "HHH-11907")
	public void testQueryStream(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			MyEntity e = new MyEntity();
			e.id = 1;
			e.name = "Test";
			session.persist( e );
		} );

		scope.inTransaction( session -> {
			// Test stream query without type.
			Object result;
			try (Stream stream = session.createQuery( "From MyEntity" ).getResultStream()) {
				result = stream.findFirst().orElse( null );
			}
			assertTyping( MyEntity.class, result );

			// Test stream query with type.
			try (Stream stream = session.createQuery( "From MyEntity", MyEntity.class ).getResultStream()) {
				result = stream.findFirst().orElse( null );
			}
			assertTyping( MyEntity.class, result );

			// Test stream query using forEach
			try (Stream<MyEntity> stream = session.createQuery( "From MyEntity", MyEntity.class ).getResultStream()) {
				stream.forEach( i -> {
					assertTyping( MyEntity.class, i );
				} );
			}

			try (Stream<Object[]> data = session.createQuery( "SELECT me.id, me.name FROM MyEntity me" )
					.getResultStream()) {
				data.forEach( i -> {
					assertTyping( Integer.class, i[0] );
					assertTyping( String.class, i[1] );
				} );
			}
		} );
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	@JiraKeyGroup( value = {
			@JiraKey( value = "HHH-13872" ),
			@JiraKey( value = "HHH-14449" )
	} )
	public void testStreamCloseOnTerminalOperation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "delete from MyEntity" ).executeUpdate();

			for ( int i = 1; i <= 10; i++ ) {
				MyEntity e = new MyEntity();
				e.id = i;
				e.name = "Test";
				session.persist( e );
			}
		} );

		Runnable noOp = () -> {
			// do nothing
		};

		// run without onClose callbacks

		this.runTerminalOperationTests( noOp, Collections.emptyList(), noOp, false, false, scope );

		AtomicInteger onClose1Count = new AtomicInteger();
		AtomicInteger onClose2Count = new AtomicInteger();
		AtomicInteger onClose3Count = new AtomicInteger();

		// run with chained onClose callbacks

		this.runTerminalOperationTests(
				() -> {
					// prepare
					onClose1Count.set( 0 );
					onClose2Count.set( 0 );
					onClose3Count.set( 0 );
				},
				Arrays.asList(
						onClose1Count::incrementAndGet, // onClose1 logic
						onClose2Count::incrementAndGet, // onClose2 logic
						onClose3Count::incrementAndGet // onClose3 logic
				),
				() -> {
					// assertion
					assertThat( onClose1Count ).hasValue( 1 );
					assertThat( onClose2Count ).hasValue( 1 );
					assertThat( onClose3Count ).hasValue( 1 );
				},
				false, // no flatMap before onClose
				false, // no flatMap after onClose
				scope
		);

		this.runTerminalOperationTests(
				() -> {
					// prepare
					onClose1Count.set( 0 );
					onClose2Count.set( 0 );
					onClose3Count.set( 0 );
				},
				Arrays.asList(
						onClose1Count::incrementAndGet, // onClose1 logic
						onClose2Count::incrementAndGet, // onClose2 logic
						onClose3Count::incrementAndGet // onClose3 logic
				),
				() -> {
					// assertion
					assertThat( onClose1Count ).hasValue( 1 );
					assertThat( onClose2Count ).hasValue( 1 );
					assertThat( onClose3Count ).hasValue( 1 );
				},
				true, // run a flatMap operation before onClose
				false, // no flatMap after onClose
				scope
		);

		this.runTerminalOperationTests(
				() -> {
					// prepare
					onClose1Count.set( 0 );
					onClose2Count.set( 0 );
					onClose3Count.set( 0 );
				},
				Arrays.asList(
						onClose1Count::incrementAndGet, // onClose1 logic
						onClose2Count::incrementAndGet, // onClose2 logic
						onClose3Count::incrementAndGet // onClose3 logic
				),
				() -> {
					// assertion
					assertThat( onClose1Count ).hasValue( 1 );
					assertThat( onClose2Count ).hasValue( 1 );
					assertThat( onClose3Count ).hasValue( 1 );
				},
				false, // no flatMap before onClose
				true, // run a flatMap operation after onClose
				scope
		);

		this.runTerminalOperationTests(
				() -> {
					// prepare
					onClose1Count.set( 0 );
					onClose2Count.set( 0 );
					onClose3Count.set( 0 );
				},
				Arrays.asList(
						onClose1Count::incrementAndGet, // onClose1 logic
						onClose2Count::incrementAndGet, // onClose2 logic
						onClose3Count::incrementAndGet // onClose3 logic
				),
				() -> {
					// assertion
					assertThat( onClose1Count ).hasValue( 1 );
					assertThat( onClose2Count ).hasValue( 1 );
					assertThat( onClose3Count ).hasValue( 1 );
				},
				true, // run a flatMap operation before onClose
				true, // run a flatMap operation after onClose
				scope
		);
	}

	private void runTerminalOperationTests(
			Runnable prepare, List<Runnable> onCloseCallbacks,
			Runnable onCloseAssertion,
			boolean flatMapBefore,
			boolean flatMapAfter,
			SessionFactoryScope scope) {

		// collect as list
		scope.inTransaction( session -> {
			Stream<MyEntity> stream = getMyEntityStream(
					prepare,
					session,
					onCloseCallbacks,
					flatMapBefore,
					flatMapAfter
			);
			ResourceRegistry resourceRegistry = resourceRegistry( session );
			try {
				List<MyEntity> entities = stream.collect( Collectors.toList() );
				assertTrue( resourceRegistry.hasRegisteredResources() );
				assertEquals( 10, entities.size() );
			}
			finally {
				stream.close();
				assertFalse( resourceRegistry.hasRegisteredResources() );
			}
			onCloseAssertion.run();
		} );

		// forEach (TestCase based on attachment EntityManagerIllustrationTest.java in HHH-14449)
		scope.inTransaction( session -> {
			Stream<MyEntity> stream = getMyEntityStream(
					prepare,
					session,
					onCloseCallbacks,
					flatMapBefore,
					flatMapAfter
			);

			ResourceRegistry resourceRegistry = resourceRegistry( session );
			try {
				AtomicInteger count = new AtomicInteger();

				stream.forEach( myEntity -> count.incrementAndGet() );
				assertTrue( resourceRegistry.hasRegisteredResources() );

				assertEquals( 10, count.get() );
			}
			finally {
				stream.close();
				assertFalse( resourceRegistry.hasRegisteredResources() );
			}

			onCloseAssertion.run();
		} );

		// filter (always true) + forEach (TestCase based on attachment EntityManagerIllustrationTest.java in HHH-14449)
		scope.inTransaction( session -> {
			Stream<MyEntity> stream = getMyEntityStream(
					prepare,
					session,
					onCloseCallbacks,
					flatMapBefore,
					flatMapAfter
			);

			ResourceRegistry resourceRegistry = resourceRegistry( session );

			try {
				AtomicInteger count = new AtomicInteger();

				stream.filter( Objects::nonNull ).forEach( myEntity -> count.incrementAndGet() );
				assertTrue( resourceRegistry.hasRegisteredResources() );
				assertEquals( 10, count.get() );
			}
			finally {
				stream.close();
				assertFalse( resourceRegistry.hasRegisteredResources() );
			}

			onCloseAssertion.run();
		} );

		// filter (partially true) + forEach (TestCase based on attachment EntityManagerIllustrationTest.java in HHH-14449)
		scope.inTransaction( session -> {
			Stream<MyEntity> stream = getMyEntityStream(
					prepare,
					session,
					onCloseCallbacks,
					flatMapBefore,
					flatMapAfter
			);

			ResourceRegistry resourceRegistry = resourceRegistry( session );

			try {
				AtomicInteger count = new AtomicInteger();

				stream.filter( entity -> entity.getId() % 2 == 0 ).forEach( myEntity -> count.incrementAndGet() );
				assertTrue( resourceRegistry.hasRegisteredResources() );
				assertEquals( 5, count.get() );
			}
			finally {
				stream.close();
				assertFalse( resourceRegistry.hasRegisteredResources() );
			}

			onCloseAssertion.run();
		} );

		// multiple chained operations (TestCase based on attachment EntityManagerIllustrationTest.java in HHH-14449)
		scope.inTransaction( session -> {
			Stream<MyEntity> stream = getMyEntityStream(
					prepare,
					session,
					onCloseCallbacks,
					flatMapBefore,
					flatMapAfter
			);

			ResourceRegistry resourceRegistry = resourceRegistry( session );

			try {
				AtomicInteger count = new AtomicInteger();

				stream
						.filter( Objects::nonNull )
						.map( Optional::of )
						.filter( Optional::isPresent )
						.map( Optional::get )
						.forEach( myEntity -> count.incrementAndGet() );
				assertTrue( resourceRegistry.hasRegisteredResources() );

				assertEquals( 10, count.get() );
			}
			finally {
				stream.close();
				assertFalse( resourceRegistry.hasRegisteredResources() );
			}

			onCloseAssertion.run();
		} );

		// mapToInt
		scope.inTransaction( session -> {
			Stream<MyEntity> stream = getMyEntityStream(
					prepare,
					session,
					onCloseCallbacks,
					flatMapBefore,
					flatMapAfter
			);

			ResourceRegistry resourceRegistry = resourceRegistry( session );
			try {
				int sum = stream.mapToInt( MyEntity::getId ).sum();
				assertTrue( resourceRegistry.hasRegisteredResources() );

				assertEquals( 55, sum );
			}
			finally {
				stream.close();
				assertFalse( resourceRegistry.hasRegisteredResources() );
			}

			onCloseAssertion.run();
		} );

		// mapToLong
		scope.inTransaction( session -> {
			Stream<MyEntity> stream = getMyEntityStream(
					prepare,
					session,
					onCloseCallbacks,
					flatMapBefore,
					flatMapAfter
			);

			ResourceRegistry resourceRegistry = resourceRegistry( session );

			try {
				long result = stream.mapToLong( entity -> entity.id * 10 ).min().getAsLong();
				assertTrue( resourceRegistry.hasRegisteredResources() );
				assertEquals( 10, result );
			}
			finally {
				stream.close();
				assertFalse( resourceRegistry.hasRegisteredResources() );
			}

			onCloseAssertion.run();
		} );

		// mapToDouble
		scope.inTransaction( session -> {
			Stream<MyEntity> stream = getMyEntityStream(
					prepare,
					session,
					onCloseCallbacks,
					flatMapBefore,
					flatMapAfter
			);

			ResourceRegistry resourceRegistry = resourceRegistry( session );
			try {
				double result = stream.mapToDouble( entity -> entity.id * 0.1D ).max().getAsDouble();
				assertTrue( resourceRegistry.hasRegisteredResources() );

				assertEquals( 1, result, 0.1 );
			}
			finally {
				stream.close();
				assertFalse( resourceRegistry.hasRegisteredResources() );
			}

			onCloseAssertion.run();
		} );

		//Test call close explicitly
		scope.inTransaction( session -> {
			try (Stream<Integer> stream = getIntegerStream(
					prepare,
					session,
					onCloseCallbacks,
					flatMapBefore,
					flatMapAfter
			)) {

				ResourceRegistry resourceRegistry = resourceRegistry( session );
				try {
					Object[] result = stream.sorted().skip( 5 ).limit( 5 ).toArray();
					assertTrue( resourceRegistry.hasRegisteredResources() );

					assertEquals( 5, result.length );
					assertEquals( 6, result[0] );
					assertEquals( 10, result[4] );

				}
				finally {
					stream.close();
					assertFalse( resourceRegistry.hasRegisteredResources() );
				}

				onCloseAssertion.run();
			}
		} );

		//Test Java 9 Stream methods
		scope.inTransaction( session -> {
			try (Stream<Integer> stream = getIntegerStream(
					prepare,
					session,
					onCloseCallbacks,
					flatMapBefore,
					flatMapAfter
			)) {

				ResourceRegistry resourceRegistry = resourceRegistry( session );
				try {

					Predicate<Integer> predicate = id -> id <= 5;

					Stream<Integer> takeWhileStream = stream.takeWhile( predicate );

					List<Integer> result = takeWhileStream.collect( Collectors.toList() );
					assertTrue( resourceRegistry.hasRegisteredResources() );

					assertEquals( 5, result.size() );
					assertTrue( result.contains( 1 ) );
					assertTrue( result.contains( 3 ) );
					assertTrue( result.contains( 5 ) );
				}
				finally {
					stream.close();
					assertFalse( resourceRegistry.hasRegisteredResources() );
				}

				onCloseAssertion.run();
			}
		} );

		scope.inTransaction( session -> {
			try (Stream<Integer> stream = getIntegerStream(
					prepare,
					session,
					onCloseCallbacks,
					flatMapBefore,
					flatMapAfter
			)) {

				ResourceRegistry resourceRegistry = resourceRegistry( session );

				Predicate<Integer> predicate = id -> id <= 5;

				Stream<Integer> dropWhileStream = stream.dropWhile( predicate );
				try {
					List<Integer> result = dropWhileStream.collect( Collectors.toList() );
					assertTrue( resourceRegistry.hasRegisteredResources() );

					assertEquals( 5, result.size() );
					assertTrue( result.contains( 6 ) );
					assertTrue( result.contains( 8 ) );
					assertTrue( result.contains( 10 ) );
				}
				finally {
					stream.close();
					assertFalse( resourceRegistry.hasRegisteredResources() );
				}

				onCloseAssertion.run();
			}
		} );
	}

	private static Stream<MyEntity> getMyEntityStream(
			Runnable prepare,
			Session session,
			List<Runnable> onCloseCallbacks,
			boolean flatMapBefore,
			boolean flatMapAfter) {
		return getStream(
				prepare,
				session,
				"SELECT me FROM MyEntity me",
				onCloseCallbacks,
				flatMapBefore,
				flatMapAfter
		);
	}

	private static Stream<Integer> getIntegerStream(
			Runnable prepare,
			Session session,
			List<Runnable> onCloseCallbacks,
			boolean flatMapBefore,
			boolean flatMapAfter) {
		return getStream(
				prepare,
				session,
				"SELECT me.id FROM MyEntity me",
				onCloseCallbacks,
				flatMapBefore,
				flatMapAfter
		);
	}

	@SuppressWarnings("unchecked")
	private static <T> Stream<T> getStream(
			Runnable prepare, Session session, String queryString,
			List<Runnable> onCloseCallbacks, boolean flatMapBefore, boolean flatMapAfter) {

		prepare.run();

		Stream<T> stream = session.createQuery( queryString ).getResultStream();

		if ( flatMapBefore ) {
			stream = stream.flatMap( Stream::of );
		}

		for ( Runnable callback : onCloseCallbacks ) {
			stream = stream.onClose( callback );
		}

		if ( flatMapAfter ) {
			stream = stream.flatMap( Stream::of );
		}

		return stream;
	}

	private ResourceRegistry resourceRegistry(Session session) {
		SharedSessionContractImplementor sharedSessionContractImplementor = (SharedSessionContractImplementor) session;
		JdbcCoordinator jdbcCoordinator = sharedSessionContractImplementor.getJdbcCoordinator();
		return jdbcCoordinator.getLogicalConnection().getResourceRegistry();
	}

	@Entity(name = "MyEntity")
	@Table(name = "MyEntity")
	public static class MyEntity {

		@Id
		public Integer id;

		public String name;

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

}
