package org.hibernate.orm.test.query.hql;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.exception.LockAcquisitionException;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An insert with a conflict clause must not fail with a unique constraint violation
 * when a concurrent transaction inserts a conflicting row.
 * This matters for databases on which the conflict clause is emulated with a {@code merge} statement.
 */
@DomainModel(annotatedClasses = InsertConflictConcurrencyTests.Item.class)
@SessionFactory
@JiraKey("HHH-21039")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUpsertOrMerge.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
public class InsertConflictConcurrencyTests {

	private static final String DO_NOTHING = "on conflict(id) do nothing";
	private static final String DO_UPDATE = "on conflict(id) do update set data = excluded.data";

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testDoNothingAfterConcurrentInsert(SessionFactoryScope scope) {
		final int updated = insertWhileConflictingRowIsUncommitted( scope, "values (1, 'second') " + DO_NOTHING );
		assertUpdateCount( scope, 0, updated );
		assertData( scope, List.of( "first" ) );
	}

	@Test
	public void testDoUpdateAfterConcurrentInsert(SessionFactoryScope scope) {
		final int updated = insertWhileConflictingRowIsUncommitted( scope, "values (1, 'second') " + DO_UPDATE );
		assertUpdateCount( scope, 1, updated );
		assertData( scope, List.of( "second" ) );
	}

	@Test
	public void testDoNothingMultipleRowsAfterConcurrentInsert(SessionFactoryScope scope) {
		final int updated = insertWhileConflictingRowIsUncommitted(
				scope,
				"values (0, 'zero'), (1, 'second'), (2, 'two') " + DO_NOTHING
		);
		assertUpdateCount( scope, 2, updated );
		assertData( scope, List.of( "zero", "first", "two" ) );
	}

	@Test
	public void testDoUpdateMultipleRowsAfterConcurrentInsert(SessionFactoryScope scope) {
		final int updated = insertWhileConflictingRowIsUncommitted(
				scope,
				"values (0, 'zero'), (1, 'second'), (2, 'two') " + DO_UPDATE
		);
		assertUpdateCount( scope, 3, updated );
		assertData( scope, List.of( "zero", "second", "two" ) );
	}

	@Test
	public void testConcurrentDoNothing(SessionFactoryScope scope) {
		insertConcurrently( scope, DO_NOTHING );
	}

	@Test
	public void testConcurrentDoUpdate(SessionFactoryScope scope) {
		insertConcurrently( scope, DO_UPDATE );
	}

	/**
	 * Inserts the row with id 1 and keeps the transaction open while another transaction
	 * runs the given insert, which then has to wait for the first transaction.
	 */
	private int insertWhileConflictingRowIsUncommitted(SessionFactoryScope scope, String valuesAndConflictClause) {
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			final List<Future<Integer>> future = new ArrayList<>( 1 );
			scope.inTransaction( session -> {
				session.persist( new Item( 1, "first" ) );
				session.flush();
				future.add( executor.submit( () -> scope.fromTransaction( otherSession ->
						otherSession.createMutationQuery( "insert into Item (id, data) " + valuesAndConflictClause )
								.executeUpdate()
				) ) );
				waitUntilInsertIsBlocked();
			} );
			return await( future.get( 0 ) );
		}
		finally {
			executor.shutdownNow();
		}
	}

	private void insertConcurrently(SessionFactoryScope scope, String conflictClause) {
		final int threads = 4;
		final int keys = 50;
		final CyclicBarrier barrier = new CyclicBarrier( threads );
		final List<Throwable> failures = new CopyOnWriteArrayList<>();
		final ExecutorService executor = Executors.newFixedThreadPool( threads );
		try {
			final List<Future<?>> futures = new ArrayList<>( threads );
			for ( int i = 0; i < threads; i++ ) {
				final String data = "thread" + i;
				futures.add( executor.submit( () -> {
					for ( int id = 0; id < keys; id++ ) {
						barrier.await( 30, TimeUnit.SECONDS );
						final int key = id;
						try {
							scope.inTransaction( session ->
									session.createMutationQuery(
													"insert into Item (id, data) values (:id, :data) " + conflictClause )
											.setParameter( "id", key )
											.setParameter( "data", data )
											.executeUpdate()
							);
						}
						catch (RuntimeException e) {
							// A deadlock is possible under this contention and is not what this test is about
							if ( !isLockConflict( e ) ) {
								failures.add( e );
							}
						}
					}
					return null;
				} ) );
			}
			for ( Future<?> future : futures ) {
				await( future );
			}
		}
		finally {
			executor.shutdownNow();
		}
		if ( !failures.isEmpty() ) {
			throw new AssertionError( failures.size() + " of " + threads * keys + " inserts failed", failures.get( 0 ) );
		}
		scope.inTransaction( session ->
				assertEquals( keys, session.createSelectionQuery( "from Item", Item.class ).getResultCount() )
		);
	}

	private static boolean isLockConflict(Throwable e) {
		for ( Throwable t = e; t != null; t = t.getCause() ) {
			if ( t instanceof LockAcquisitionException ) {
				return true;
			}
		}
		return false;
	}

	private static void waitUntilInsertIsBlocked() {
		// Give the concurrent transaction enough time to reach the conflicting row
		try {
			Thread.sleep( 2_000L );
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
	}

	private static <T> T await(Future<T> future) {
		try {
			return future.get( 2, TimeUnit.MINUTES );
		}
		catch (ExecutionException e) {
			throw new AssertionError( "Insert failed", e.getCause() );
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	private static void assertUpdateCount(SessionFactoryScope scope, int expected, int updated) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		// These databases report the matched rows as affected (see InsertConflictTests)
		if ( !( dialect instanceof MySQLDialect || dialect instanceof SybaseASEDialect ) ) {
			assertEquals( expected, updated );
		}
	}

	private static void assertData(SessionFactoryScope scope, List<String> expected) {
		scope.inTransaction( session -> assertEquals(
				expected,
				session.createSelectionQuery( "select data from Item order by id", String.class ).getResultList()
		) );
	}

	@Entity(name = "Item")
	public static class Item {
		@Id
		public Integer id;
		public String data;

		public Item() {
		}

		public Item(Integer id, String data) {
			this.id = id;
			this.data = data;
		}
	}
}
