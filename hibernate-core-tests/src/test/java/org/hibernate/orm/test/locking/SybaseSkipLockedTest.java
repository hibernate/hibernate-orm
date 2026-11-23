package org.hibernate.orm.test.locking;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.LockModeType;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.Timeouts;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.lock.internal.LockingSqlRewriterSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.SpecHints;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/// Verifies that ASE skips incompatible locks while retaining protection on returned rows.
///
/// @author Steve Ebersole
@RequiresDialect(SybaseASEDialect.class)
@DomainModel(annotatedClasses = {
		SybaseSkipLockedTest.WorkItem.class,
		SybaseSkipLockedTest.PolymorphicWork.class,
		SybaseSkipLockedTest.FirstWork.class,
		SybaseSkipLockedTest.SecondWork.class
})
@SessionFactory(useCollectingStatementObserver = true)
@ServiceRegistry(settings = {
		@Setting(name = JdbcSettings.ISOLATION, value = "READ_COMMITTED"),
		// Keep fresh jTDS connections in ASE's non-autocommit transaction mode.
		@Setting(name = "hibernate.connection.init_sql", value = "set ansinull on set chained on")
})
class SybaseSkipLockedTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new WorkItem( 1 ) );
			session.persist( new WorkItem( 2 ) );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	static Stream<Arguments> lockingRequests() {
		return Stream.of( LockModeType.PESSIMISTIC_READ, LockModeType.PESSIMISTIC_WRITE )
				.flatMap( mode -> Stream.of( Access.values() ).map( access -> Arguments.of( mode, access ) ) );
	}

	static Stream<Arguments> competingRequests() {
		return Stream.of( LockModeType.PESSIMISTIC_READ, LockModeType.PESSIMISTIC_WRITE )
				.flatMap( first -> Stream.of( LockModeType.PESSIMISTIC_READ, LockModeType.PESSIMISTIC_WRITE )
						.map( second -> Arguments.of( first, second ) ) );
	}

	@ParameterizedTest
	@MethodSource("competingRequests")
	void competingSelectorsRespectLockMode(LockModeType firstMode, LockModeType secondMode, SessionFactoryScope scope) {
		scope.inTransaction( first -> {
			var selected = first.createQuery( "from AseWorkItem where id = 2", WorkItem.class )
					.setLockMode( firstMode )
					.setHint( SpecHints.HINT_SPEC_LOCK_TIMEOUT, Timeouts.SKIP_LOCKED ).getResultList();
			assertEquals( 1, selected.size() );
			scope.inTransaction( second -> {
				setLockWait( second, "set lock wait 2" );
				try {
					var skipped = second.createQuery( "from AseWorkItem where id = 2", WorkItem.class )
							.setLockMode( secondMode )
							.setHint( SpecHints.HINT_SPEC_LOCK_TIMEOUT, Timeouts.SKIP_LOCKED ).getResultList();
					assertEquals( firstMode == LockModeType.PESSIMISTIC_READ && secondMode == LockModeType.PESSIMISTIC_READ
							? 1 : 0, skipped.size() );
				}
				finally {
					setLockWait( second, "set lock wait" );
				}
			} );
			assertUpdateBlocked( scope );
		} );
	}

	static Stream<Arguments> readSetQueries() {
		return Stream.of( "union", "union all" )
				.flatMap( operator -> Stream.of( false, true ).map( limited -> Arguments.of( operator, limited ) ) );
	}

	@ParameterizedTest
	@MethodSource("readSetQueries")
	void readLockSetQuerySkipsLockedRowsInEveryArm(String operator, boolean limited, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new WorkItem( 3 ) );
			session.persist( new WorkItem( 4 ) );
		} );
		scope.inTransaction( holder -> {
			holder.createMutationQuery( "update AseWorkItem set marker = 1 where id in (1, 3)" ).executeUpdate();
			scope.inTransaction( reader -> {
				setLockWait( reader, "set lock wait 2" );
				try {
					var query = reader.createQuery(
							"(select w from AseWorkItem w where w.id <= 2 " + operator
									+ " select x from AseWorkItem x where x.id >= 3) order by 1", WorkItem.class )
							.setLockMode( LockModeType.PESSIMISTIC_READ )
							.setHint( SpecHints.HINT_SPEC_LOCK_TIMEOUT, Timeouts.SKIP_LOCKED );
					if ( limited ) {
						query.setMaxResults( 1 );
					}
					var selected = query.getResultList();
					assertEquals( limited ? List.of( 2 ) : List.of( 2, 4 ), selected.stream().map( item -> item.id ).toList() );
					assertUpdateBlocked( scope );
					if ( !limited ) {
						assertUpdateBlocked( scope, 4 );
					}
				}
				finally {
					setLockWait( reader, "set lock wait" );
				}
			} );
		} );
	}

	@Test
	void disabledServerOptionDoesNotDowngradeToSharedLock(SessionFactoryScope scope) {
		boolean enabled = scope.fromTransaction( session -> session.createNativeQuery(
				"select value from master..syscurconfigs where config in "
						+ "(select config from master..sysconfigures where name = 'select for update')", Integer.class )
				.getSingleResult() == 1 );
		assumeFalse( enabled, "Requires an ASE instance with select for update disabled" );
		var failure = assertThrows( IllegalStateException.class, () -> scope.inTransaction( session ->
				session.createQuery( "from AseWorkItem where id = 2", WorkItem.class )
						.setLockMode( LockModeType.PESSIMISTIC_WRITE )
						.setHint( SpecHints.HINT_SPEC_LOCK_TIMEOUT, Timeouts.SKIP_LOCKED ).getResultList() ) );
		assertTrue( failure.getCause() instanceof org.hibernate.query.IllegalQueryOperationException );
		assertTrue( failure.getMessage().contains( "requires the server option 'select for update'" ) );
	}

	static Stream<Arguments> unsupportedExclusiveShapes() {
		return Stream.of(
				Arguments.of( "select distinct w from AseWorkItem w", "DISTINCT" ),
				Arguments.of( "from AsePolymorphicWork", "union table references" ),
				Arguments.of( "select w from AseWorkItem w full join AseWorkItem x on w.id=x.id", "FULL JOIN emulation" ),
				Arguments.of( "select w from AseWorkItem w group by w", "GROUP BY" ),
				Arguments.of( "select w from AseWorkItem w group by w having count(w) > 0", "HAVING" ),
				Arguments.of( "select w, count(w) from AseWorkItem w", "aggregate functions" ),
				Arguments.of( "select w from AseWorkItem w where exists (select x.id from AseWorkItem x where x.id=w.id)", "subqueries" ),
				Arguments.of( "select w from AseWorkItem w where w.id in (select x.id from AseWorkItem x)", "subqueries" ),
				Arguments.of( "select w, (select max(x.id) from AseWorkItem x) from AseWorkItem w", "subqueries" )
		);
	}

	@ParameterizedTest
	@MethodSource("unsupportedExclusiveShapes")
	void unsupportedExclusiveShapeIsRejectedBeforeExecution(String hql, String shape, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var observer = scope.getCollectingStatementObserver();
			observer.clear();
			var failure = assertThrows( IllegalStateException.class, () -> session.createQuery( hql, Object.class )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.setHint( SpecHints.HINT_SPEC_LOCK_TIMEOUT, Timeouts.SKIP_LOCKED ).getResultList() );
			assertTrue( failure.getCause() instanceof org.hibernate.query.IllegalQueryOperationException );
			assertTrue( failure.getMessage().contains( "exclusive skip-locked locking with " + shape ), failure::toString );
			assertTrue( observer.getSqlQueries().isEmpty(), "Unsupported locking query must not reach JDBC" );
		} );
	}

	@Test
	void distinctReadSelectionRetainsSharedProtection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var result = session.createQuery( "select distinct w from AseWorkItem w order by w.id", WorkItem.class )
					.setLockMode( LockModeType.PESSIMISTIC_READ )
					.setHint( SpecHints.HINT_SPEC_LOCK_TIMEOUT, Timeouts.SKIP_LOCKED ).getResultList();
			assertEquals( List.of( 1, 2 ), result.stream().map( item -> item.id ).toList() );
			assertUpdateBlocked( scope );
		} );
	}

	@Test
	void concreteTablePerClassSelectionIsAllowed(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertTrue( session.createQuery( "from AseFirstWork", FirstWork.class )
				.setLockMode( LockModeType.PESSIMISTIC_WRITE )
				.setHint( SpecHints.HINT_SPEC_LOCK_TIMEOUT, Timeouts.SKIP_LOCKED ).getResultList().isEmpty() ) );
	}

	@Test
	void calculatedSelectionRetainsExclusiveProtection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var result = session.createQuery( "select w, w.id + 1 from AseWorkItem w where w.id=2", Object[].class )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.setHint( SpecHints.HINT_SPEC_LOCK_TIMEOUT, Timeouts.SKIP_LOCKED ).getSingleResult();
			assertEquals( 2, ((WorkItem) result[0]).id );
			assertEquals( 3, result[1] );
			assertUpdateBlocked( scope );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "union", "union all" })
	void exclusiveSetQueryIsRejectedInsteadOfDowngrading(String operator, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var failure = assertThrows( RuntimeException.class, () -> session.createQuery(
					"select w from AseWorkItem w where w.id = 1 " + operator
							+ " select x from AseWorkItem x where x.id = 2", WorkItem.class )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.setHint( SpecHints.HINT_SPEC_LOCK_TIMEOUT, Timeouts.SKIP_LOCKED ).getResultList() );
			assertTrue( failure.getMessage().contains( "exclusive skip-locked locking of set queries" ) );
		} );
	}

	@ParameterizedTest
	@MethodSource("lockingRequests")
	void skipsLockedRowsAndProtectsReturnedRows(LockModeType mode, Access access, SessionFactoryScope scope) {
		scope.inTransaction( holder -> {
			// An actual write establishes an incompatible lock even when ASE uses shared read locks.
			holder.createMutationQuery( "update AseWorkItem set marker = 1 where id = 1" ).executeUpdate();
			scope.inTransaction( selector -> {
				setLockWait( selector, "set lock wait 2" );
				try {
					selector.doWork( connection -> assertEquals( Connection.TRANSACTION_READ_COMMITTED,
							connection.getTransactionIsolation() ) );
					List<WorkItem> selected = select( selector, mode, access );
					assertEquals( List.of( 2 ), selected.stream().map( item -> item.id ).toList() );
					if ( access != Access.REWRITTEN_SQL ) {
						assertEquals( mode, selector.getLockMode( selected.get( 0 ) ) );
					}
					selector.doWork( connection -> {
						try ( var statement = connection.createStatement();
								var result = statement.executeQuery( "select @@trancount" ) ) {
							result.next();
							assertEquals( 1, result.getInt( 1 ), "selector must retain its database transaction" );
						}
					} );
					assertUpdateBlocked( scope );
					selector.doWork( connection -> assertEquals( Connection.TRANSACTION_READ_COMMITTED,
							connection.getTransactionIsolation() ) );
				}
				finally {
					setLockWait( selector, "set lock wait" );
				}
			} );
			// Completion of the selector transaction must release the retained lock.
			scope.inTransaction( writer -> assertEquals( 1,
					writer.createMutationQuery( "update AseWorkItem set marker = 2 where id = 2" ).executeUpdate() ) );
		} );
	}

	private void setLockWait(Session session, String sql) {
		session.doWork( connection -> {
			try ( var statement = connection.createStatement() ) {
				statement.execute( sql );
			}
		} );
	}

	private List<WorkItem> select(Session session, LockModeType mode, Access access) {
		var hints = Map.<String, Object>of( SpecHints.HINT_SPEC_LOCK_TIMEOUT, Timeouts.SKIP_LOCKED_MILLI );
		return switch ( access ) {
			case QUERY -> session.createQuery( "from AseWorkItem order by id", WorkItem.class )
					.setLockMode( mode )
					.setHint( SpecHints.HINT_SPEC_LOCK_TIMEOUT, Timeouts.SKIP_LOCKED )
					.getResultList();
			case REWRITTEN_SQL -> {
				var support = session.getSessionFactory().unwrap( SessionFactoryImplementor.class )
						.getJdbcServices().getDialect().getLockingSupport();
				var options = new LockOptions( LockMode.fromJpaLockMode( mode ) ).setTimeout( Timeouts.SKIP_LOCKED );
				var rewritten = LockingSqlRewriterSupport.rewrite( support,
						"select w.* from AseWorkItem w order by w.id", options, Map.of( "w", new String[] { "id" } ) );
				yield session.createNativeQuery( rewritten.sql(), WorkItem.class ).getResultList();
			}
			case FIND -> List.of( session.find( WorkItem.class, 2, mode, hints ) );
			case REFRESH -> {
				WorkItem item = session.find( WorkItem.class, 2 );
				assertNotNull( item );
				session.refresh( item, mode, hints );
				yield List.of( item );
			}
		};
	}

	private void assertUpdateBlocked(SessionFactoryScope scope) {
		assertUpdateBlocked( scope, 2 );
	}

	private void assertUpdateBlocked(SessionFactoryScope scope, int id) {
		scope.inTransaction( writer -> writer.doWork( connection -> {
			try ( var statement = connection.createStatement() ) {
				statement.execute( "set lock wait 0" );
				try {
					SQLException failure = assertThrows( SQLException.class,
							() -> statement.executeUpdate( "update AseWorkItem set marker = 3 where id = " + id ) );
					assertEquals( 12205, failure.getErrorCode() );
				}
				finally {
					statement.execute( "set lock wait" );
				}
			}
		} ) );
	}

	enum Access { QUERY, REWRITTEN_SQL, FIND, REFRESH }

	@Entity(name = "AsePolymorphicWork")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	static abstract class PolymorphicWork {
		@Id
		int id;
	}

	@Entity(name = "AseFirstWork")
	static class FirstWork extends PolymorphicWork {
	}

	@Entity(name = "AseSecondWork")
	static class SecondWork extends PolymorphicWork {
	}

	@Entity(name = "AseWorkItem")
	static class WorkItem {
		@Id
		int id;
		int marker;

		WorkItem() {
		}

		WorkItem(int id) {
			this.id = id;
		}
	}
}
