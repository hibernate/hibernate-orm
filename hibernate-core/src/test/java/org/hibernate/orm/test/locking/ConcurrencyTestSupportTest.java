package org.hibernate.orm.test.locking;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Guards against accepting Java scheduling delay as proof of a database lock.
/// H2 is an isolated physical fixture here, independent of the selected dialect.
///
/// @author Steve Ebersole
class ConcurrencyTestSupportTest {
	@Test
	void delayedDispatchWithoutOwnerLockFailsTheProof() throws Exception {
		try ( var connection = DriverManager.getConnection( "jdbc:h2:mem:" + UUID.randomUUID() ) ) {
			seed( connection );
			final var released = new AtomicBoolean();
			final var dispatchedBeforeRelease = new AtomicBoolean();
			final var executions = new AtomicInteger();
			final Connection delayed = (Connection) Proxy.newProxyInstance( Connection.class.getClassLoader(),
					new Class<?>[] { Connection.class }, (proxy, method, arguments) -> {
						try {
							final var value = method.invoke( connection, arguments );
							if ( method.getName().equals( "prepareStatement" ) ) {
								return Proxy.newProxyInstance( PreparedStatement.class.getClassLoader(),
										new Class<?>[] { PreparedStatement.class }, (p, m, a) -> {
											try {
												if ( m.getName().equals( "executeUpdate" ) && executions.getAndIncrement() == 0 ) {
													Thread.sleep( 600 ); // exceeds the former 200 ms false-positive window
													dispatchedBeforeRelease.set( !released.get() );
												}
												return m.invoke( value, a );
											}
											catch (InvocationTargetException failure) { throw failure.getCause(); }
										} );
							}
							return value;
						}
						catch (InvocationTargetException failure) { throw failure.getCause(); }
					} );
			assertThrows( AssertionError.class, () -> ConcurrencyTestSupport.assertWriteProtected(
					factory( delayed ), "sample", 1, () -> released.set( true ) ) );
			assertTrue( dispatchedBeforeRelease.get(), "A delayed contender must still execute before owner release" );
			assertTrue( released.get(), "Assertion failure must release the owner" );
			assertEquals( 0, revision( connection ), "Failed proof must roll back the contender" );
			assertEquals( 1897, timeout( connection ), "Restore the native timeout after failure" );
		}
	}

	@Test
	void realLockConflictAndReleaseAreRequired() throws Exception {
		final var url = "jdbc:h2:mem:" + UUID.randomUUID();
		try ( var owner = DriverManager.getConnection( url ); var contender = DriverManager.getConnection( url ) ) {
			seed( owner );
			try ( var statement = contender.createStatement() ) { statement.execute( "set lock_timeout 1897" ); }
			owner.setAutoCommit( false );
			try ( var statement = owner.createStatement() ) { statement.executeUpdate( "update sample set revision=1 where id=1" ); }
			ConcurrencyTestSupport.assertWriteProtected( factory( contender ), "sample", 1, () -> {
				try { owner.commit(); }
				catch (java.sql.SQLException failure) { throw new IllegalStateException( failure ); }
			} );
			assertEquals( 1, revision( contender ), "Only the owner's update persists" );
			assertEquals( 1897, timeout( contender ), "Restore the native timeout after a real conflict" );
		}
	}

	private static SessionFactoryImplementor factory(Connection connection) throws Exception {
		final var factory = mock( SessionFactoryImplementor.class, RETURNS_DEEP_STUBS );
		when( factory.getJdbcServices().getDialect() ).thenReturn( new H2Dialect() );
		when( factory.getJdbcServices().getBootstrapJdbcConnectionAccess().obtainConnection() ).thenReturn( connection );
		when( factory.getJdbcServices().getJdbcEnvironment().getJdbcMetadata().getTransactionIsolation() ).thenReturn( 2 );
		return factory;
	}

	private static void seed(Connection connection) throws Exception {
		try ( var statement = connection.createStatement() ) {
			statement.execute( "create table sample (id int primary key, revision int)" );
			statement.execute( "insert into sample values (1,0)" );
			statement.execute( "set lock_timeout 1897" );
		}
	}

	private static int timeout(Connection connection) throws Exception {
		try ( var statement = connection.createStatement(); var rows = statement.executeQuery( "call lock_timeout()" ) ) {
			assertTrue( rows.next() );
			return rows.getInt( 1 );
		}
	}

	private static int revision(Connection connection) throws Exception {
		return ConcurrencyTestSupport.revision( connection, "select revision from sample where id=?", 1 );
	}
}
