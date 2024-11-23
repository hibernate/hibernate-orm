/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.stream.basic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.resource.jdbc.ResourceRegistry;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class JpaStreamTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11907")
	public void testQueryStream() {
		doInHibernate( this::sessionFactory, session -> {
			MyEntity e= new MyEntity();
			e.id = 1;
			e.name = "Test";
			session.persist( e );
		} );

		doInHibernate( this::sessionFactory, session -> {
			// Test stream query without type.
			Object result = session.createQuery( "From MyEntity" ).getResultStream().findFirst().orElse( null );
			assertTyping( MyEntity.class, result );

			// Test stream query with type.
			result = session.createQuery( "From MyEntity", MyEntity.class ).getResultStream().findFirst().orElse( null );
			assertTyping( MyEntity.class, result );

			// Test stream query using forEach
			session.createQuery( "From MyEntity", MyEntity.class ).getResultStream().forEach( i -> {
				assertTyping( MyEntity.class, i );
			} );

			Stream<Object[]> data = session.createQuery( "SELECT me.id, me.name FROM MyEntity me" ).getResultStream();
			data.forEach( i -> {
				assertTyping( Integer.class, i[0] );
				assertTyping( String.class, i[1] );
			});
		} );
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	@TestForIssue( jiraKey = {"HHH-13872", "HHH-14449"})
	public void testStreamCloseOnTerminalOperation() {
		doInHibernate( this::sessionFactory, session -> {
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

		this.runTerminalOperationTests(noOp, Collections.emptyList(), noOp, false, false);

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
				false // no flatMap after onClose
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
				false // no flatMap after onClose
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
				true // run a flatMap operation after onClose
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
				true // run a flatMap operation after onClose
		);
	}

	private void runTerminalOperationTests(
			Runnable prepare, List<Runnable> onCloseCallbacks, Runnable onCloseAssertion,
			boolean flatMapBefore, boolean flatMapAfter) {

		// collect as list
		doInHibernate( this::sessionFactory, session -> {
			Stream<MyEntity> stream = getMyEntityStream(prepare, session, onCloseCallbacks, flatMapBefore, flatMapAfter);

			ResourceRegistry resourceRegistry = resourceRegistry(session);
			assertTrue( resourceRegistry.hasRegisteredResources() );

			List<MyEntity> entities = stream.collect( Collectors.toList() ) ;
			assertEquals(10, entities.size());

			assertFalse( resourceRegistry.hasRegisteredResources() );

			onCloseAssertion.run();
		} );

		// forEach (TestCase based on attachment EntityManagerIllustrationTest.java in HHH-14449)
		doInHibernate( this::sessionFactory, session -> {
			Stream<MyEntity> stream = getMyEntityStream(prepare, session, onCloseCallbacks, flatMapBefore, flatMapAfter);

			ResourceRegistry resourceRegistry = resourceRegistry(session);
			assertTrue( resourceRegistry.hasRegisteredResources() );

			AtomicInteger count = new AtomicInteger();

			stream.forEach(myEntity -> count.incrementAndGet());

			assertEquals(10, count.get());

			assertFalse( resourceRegistry.hasRegisteredResources() );

			onCloseAssertion.run();
		} );

		// filter (always true) + forEach (TestCase based on attachment EntityManagerIllustrationTest.java in HHH-14449)
		doInHibernate( this::sessionFactory, session -> {
			Stream<MyEntity> stream = getMyEntityStream(prepare, session, onCloseCallbacks, flatMapBefore, flatMapAfter);

			ResourceRegistry resourceRegistry = resourceRegistry(session);
			assertTrue( resourceRegistry.hasRegisteredResources() );

			AtomicInteger count = new AtomicInteger();

			stream.filter(Objects::nonNull).forEach(myEntity -> count.incrementAndGet());

			assertEquals(10, count.get());

			assertFalse( resourceRegistry.hasRegisteredResources() );

			onCloseAssertion.run();
		} );

		// filter (partially true) + forEach (TestCase based on attachment EntityManagerIllustrationTest.java in HHH-14449)
		doInHibernate( this::sessionFactory, session -> {
			Stream<MyEntity> stream = getMyEntityStream(prepare, session, onCloseCallbacks, flatMapBefore, flatMapAfter);

			ResourceRegistry resourceRegistry = resourceRegistry(session);
			assertTrue( resourceRegistry.hasRegisteredResources() );

			AtomicInteger count = new AtomicInteger();

			stream.filter(entity -> entity.getId() % 2 == 0).forEach(myEntity -> count.incrementAndGet());

			assertEquals(5, count.get());

			assertFalse( resourceRegistry.hasRegisteredResources() );

			onCloseAssertion.run();
		} );

		// multiple chained operations (TestCase based on attachment EntityManagerIllustrationTest.java in HHH-14449)
		doInHibernate( this::sessionFactory, session -> {
			Stream<MyEntity> stream = getMyEntityStream(prepare, session, onCloseCallbacks, flatMapBefore, flatMapAfter);

			ResourceRegistry resourceRegistry = resourceRegistry(session);
			assertTrue( resourceRegistry.hasRegisteredResources() );

			AtomicInteger count = new AtomicInteger();

			stream
					.filter(Objects::nonNull)
					.map(Optional::of)
					.filter(Optional::isPresent)
					.map(Optional::get)
					.forEach(myEntity -> count.incrementAndGet());

			assertEquals(10, count.get());

			assertFalse( resourceRegistry.hasRegisteredResources() );

			onCloseAssertion.run();
		} );

		// mapToInt
		doInHibernate( this::sessionFactory, session -> {
			Stream<MyEntity> stream = getMyEntityStream(prepare, session, onCloseCallbacks, flatMapBefore, flatMapAfter);

			ResourceRegistry resourceRegistry = resourceRegistry(session);
			assertTrue( resourceRegistry.hasRegisteredResources() );

			int sum = stream.mapToInt( MyEntity::getId ).sum();
			assertEquals(55, sum);

			assertFalse( resourceRegistry.hasRegisteredResources() );

			onCloseAssertion.run();
		} );

		// mapToLong
		doInHibernate( this::sessionFactory, session -> {
			Stream<MyEntity> stream = getMyEntityStream(prepare, session, onCloseCallbacks, flatMapBefore, flatMapAfter);

			ResourceRegistry resourceRegistry = resourceRegistry(session);
			assertTrue( resourceRegistry.hasRegisteredResources() );

			long result = stream.mapToLong( entity -> entity.id * 10 ).min().getAsLong();
			assertEquals(10, result);

			assertFalse( resourceRegistry.hasRegisteredResources() );

			onCloseAssertion.run();
		} );

		// mapToDouble
		doInHibernate( this::sessionFactory, session -> {
			Stream<MyEntity> stream = getMyEntityStream(prepare, session, onCloseCallbacks, flatMapBefore, flatMapAfter);

			ResourceRegistry resourceRegistry = resourceRegistry(session);
			assertTrue( resourceRegistry.hasRegisteredResources() );

			double result = stream.mapToDouble( entity -> entity.id * 0.1D ).max().getAsDouble();
			assertEquals(1, result, 0.1);

			assertFalse( resourceRegistry.hasRegisteredResources() );

			onCloseAssertion.run();
		} );

		//Test call close explicitly
		doInHibernate( this::sessionFactory, session -> {

			try (Stream<Long> stream = getLongStream(prepare, session, onCloseCallbacks, flatMapBefore, flatMapAfter)) {

				ResourceRegistry resourceRegistry = resourceRegistry( session );
				assertTrue( resourceRegistry.hasRegisteredResources() );

				Object[] result = stream.sorted().skip( 5 ).limit( 5 ).toArray();
				assertEquals( 5, result.length );
				assertEquals( 6, result[0] );
				assertEquals( 10, result[4] );

				assertFalse( resourceRegistry.hasRegisteredResources() );

				onCloseAssertion.run();
			}
		} );

		//Test Java 9 Stream methods
		doInHibernate( this::sessionFactory, session -> {
			Method takeWhileMethod = ReflectHelper.getMethod( Stream.class, "takeWhile", Predicate.class );

			if ( takeWhileMethod != null ) {
				try (Stream<Long> stream = getLongStream(prepare, session, onCloseCallbacks, flatMapBefore, flatMapAfter)) {

					ResourceRegistry resourceRegistry = resourceRegistry( session );
					assertTrue( resourceRegistry.hasRegisteredResources() );

					Predicate<Integer> predicate = id -> id <= 5;

					Stream<Integer> takeWhileStream = (Stream<Integer>) takeWhileMethod.invoke( stream, predicate );

					List<Integer> result = takeWhileStream.collect( Collectors.toList() );

					assertEquals( 5, result.size() );
					assertTrue( result.contains( 1 ) );
					assertTrue( result.contains( 3 ) );
					assertTrue( result.contains( 5 ) );

					assertFalse( resourceRegistry.hasRegisteredResources() );

					onCloseAssertion.run();
				}
				catch (IllegalAccessException | InvocationTargetException e) {
					fail( "Could not execute takeWhile because of " + e.getMessage() );
				}
			}
		} );

		doInHibernate( this::sessionFactory, session -> {
			Method dropWhileMethod = ReflectHelper.getMethod( Stream.class, "dropWhile", Predicate.class );

			if ( dropWhileMethod != null ) {
				try (Stream<Long> stream = getLongStream(prepare, session, onCloseCallbacks, flatMapBefore, flatMapAfter)) {

					ResourceRegistry resourceRegistry = resourceRegistry( session );
					assertTrue( resourceRegistry.hasRegisteredResources() );

					Predicate<Integer> predicate = id -> id <= 5;

					Stream<Integer> dropWhileStream = (Stream<Integer>) dropWhileMethod.invoke( stream, predicate );

					List<Integer> result = dropWhileStream.collect( Collectors.toList() );

					assertEquals( 5, result.size() );
					assertTrue( result.contains( 6 ) );
					assertTrue( result.contains( 8 ) );
					assertTrue( result.contains( 10 ) );

					assertFalse( resourceRegistry.hasRegisteredResources() );

					onCloseAssertion.run();
				}
				catch (IllegalAccessException | InvocationTargetException e) {
					fail( "Could not execute takeWhile because of " + e.getMessage() );
				}
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

	private static Stream<Long> getLongStream(
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
	@Table(name="MyEntity")
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
