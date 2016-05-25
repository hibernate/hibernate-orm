package org.hibernate.engine.jdbc.internal;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.resource.jdbc.spi.JdbcObserver;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.service.ServiceRegistry;

import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Vlad Mihalcea
 */
public class JdbcCoordinatorTest {

	@Test
	public void testConnectionClose()
			throws NoSuchFieldException, IllegalAccessException, SQLException {
		Connection connection = Mockito.mock( Connection.class );

		JdbcSessionOwner sessionOwner = Mockito.mock( JdbcSessionOwner.class );

		JdbcConnectionAccess jdbcConnectionAccess = Mockito.mock(
				JdbcConnectionAccess.class );
		when( jdbcConnectionAccess.obtainConnection() ).thenReturn( connection );
		when( jdbcConnectionAccess.supportsAggressiveRelease() ).thenReturn(
				false );

		JdbcSessionContext sessionContext = Mockito.mock( JdbcSessionContext.class );
		when( sessionOwner.getJdbcSessionContext() ).thenReturn( sessionContext );
		when( sessionOwner.getJdbcConnectionAccess() ).thenReturn(
				jdbcConnectionAccess );

		ServiceRegistry serviceRegistry = Mockito.mock( ServiceRegistry.class );
		when( sessionContext.getServiceRegistry() ).thenReturn( serviceRegistry );
		when( sessionContext.getPhysicalConnectionHandlingMode() ).thenReturn(
				PhysicalConnectionHandlingMode.IMMEDIATE_ACQUISITION_AND_HOLD );

		JdbcObserver jdbcObserver = Mockito.mock( JdbcObserver.class );
		when( sessionContext.getObserver() ).thenReturn( jdbcObserver );

		JdbcServices jdbcServices = Mockito.mock( JdbcServices.class );
		when( serviceRegistry.getService( eq( JdbcServices.class ) ) ).thenReturn(
				jdbcServices );

		SqlExceptionHelper sqlExceptionHelper = Mockito.mock( SqlExceptionHelper.class );
		when( jdbcServices.getSqlExceptionHelper() ).thenReturn(
				sqlExceptionHelper );

		JdbcCoordinatorImpl jdbcCoordinator = new JdbcCoordinatorImpl(
				null,
				sessionOwner
		);

		Batch currentBatch = Mockito.mock( Batch.class );
		Field currentBatchField = JdbcCoordinatorImpl.class.getDeclaredField(
				"currentBatch" );
		currentBatchField.setAccessible( true );
		currentBatchField.set( jdbcCoordinator, currentBatch );

		doThrow( IllegalStateException.class ).when( currentBatch ).release();

		try {
			jdbcCoordinator.close();
			fail( "Should throw IllegalStateException" );
		}
		catch (Exception expected) {
			assertEquals( IllegalStateException.class, expected.getClass() );
		}
		verify( jdbcConnectionAccess, times( 1 ) ).releaseConnection( same(
				connection ) );
	}
}