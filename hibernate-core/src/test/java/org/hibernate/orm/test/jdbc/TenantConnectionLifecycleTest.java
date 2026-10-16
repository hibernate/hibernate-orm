package org.hibernate.orm.test.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.resource.jdbc.internal.LogicalConnectionManagedImpl;
import org.hibernate.resource.jdbc.internal.ResourceRegistryStandardImpl;
import org.hibernate.resource.jdbc.spi.JdbcEventHandler;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_HOLD;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/// Verifies cleanup when tenant connection initialization or restoration fails.
///
/// @author Steve Ebersole
@BaseUnitTest
class TenantConnectionLifecycleTest {
	@Test
	void unsupportedSchemaOperationReleasesConnection() throws SQLException {
		final var fixture = new Fixture();
		doThrow( new java.sql.SQLFeatureNotSupportedException( "schema operations unavailable", "0A000" ) )
				.when( fixture.owner ).afterObtainConnection( fixture.connection );
		assertThrows( HibernateException.class, fixture.logical::getPhysicalConnection );
		assertFalse( fixture.logical.isPhysicallyConnected() );
		fixture.logical.close();
		verify( fixture.access, times( 1 ) ).releaseConnection( fixture.connection );
	}

	@Test
	void initializationFailureRestoresAndReleasesOnce() throws SQLException {
		final var fixture = new Fixture();
		final var initializationFailure = new HibernateException( "invalid tenant schema" );
		doThrow( initializationFailure ).when( fixture.owner ).afterObtainConnection( fixture.connection );
		final var cleanupFailure = new SQLException( "schema restoration failed" );
		doThrow( cleanupFailure ).when( fixture.owner ).beforeReleaseConnection( fixture.connection );
		final var releaseFailure = new SQLException( "release failed" );
		doThrow( releaseFailure ).when( fixture.access ).releaseConnection( fixture.connection );

		assertSame( initializationFailure,
				assertThrows( HibernateException.class, fixture.logical::getPhysicalConnection ) );
		assertArrayEquals( new Throwable[] { cleanupFailure, releaseFailure }, initializationFailure.getSuppressed() );
		assertFalse( fixture.logical.isPhysicallyConnected() );
		fixture.logical.close();
		final var order = inOrder( fixture.owner, fixture.access );
		order.verify( fixture.access ).obtainConnection();
		order.verify( fixture.owner ).afterObtainConnection( fixture.connection );
		order.verify( fixture.owner ).beforeReleaseConnection( fixture.connection );
		order.verify( fixture.access ).releaseConnection( fixture.connection );
		verify( fixture.access, times( 1 ) ).releaseConnection( fixture.connection );
	}

	@Test
	void restorationFailureStillReleasesConnectionAndIsReported() throws SQLException {
		final var fixture = new Fixture();
		fixture.logical.getPhysicalConnection();
		final var restorationFailure = new HibernateException( "schema restoration failed" );
		doThrow( restorationFailure ).when( fixture.owner ).beforeReleaseConnection( fixture.connection );

		assertSame( restorationFailure,
				assertThrows( HibernateException.class, fixture.logical::manualDisconnect ) );
		assertFalse( fixture.logical.isPhysicallyConnected() );
		fixture.logical.close();
		verify( fixture.access, times( 1 ) ).releaseConnection( fixture.connection );
	}

	private static class Fixture {
		final JdbcSessionOwner owner = mock( JdbcSessionOwner.class );
		final JdbcConnectionAccess access = mock( JdbcConnectionAccess.class );
		final Connection connection = mock( Connection.class );
		final LogicalConnectionManagedImpl logical;

		Fixture() throws SQLException {
			final var context = mock( JdbcSessionContext.class );
			when( owner.getJdbcSessionContext() ).thenReturn( context );
			when( owner.getJdbcConnectionAccess() ).thenReturn( access );
			when( access.obtainConnection() ).thenReturn( connection );
			when( context.getPhysicalConnectionHandlingMode() ).thenReturn( DELAYED_ACQUISITION_AND_HOLD );
			when( context.getEventHandler() ).thenReturn( mock( JdbcEventHandler.class ) );
			final var services = mock( JdbcServices.class );
			when( context.getJdbcServices() ).thenReturn( services );
			final var helper = new SqlExceptionHelper( false );
			when( services.getSqlExceptionHelper() ).thenReturn( helper );
			when( owner.getSqlExceptionHelper() ).thenReturn( helper );
			logical = new LogicalConnectionManagedImpl( owner, new ResourceRegistryStandardImpl() );
		}
	}
}
