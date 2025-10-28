/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.internal.JdbcCoordinatorImpl;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.resource.jdbc.spi.JdbcEventHandler;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.resource.jdbc.spi.JdbcSessionOwner;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.service.ServiceRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
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
		when( sessionContext.getPhysicalConnectionHandlingMode() ).thenReturn(
				PhysicalConnectionHandlingMode.IMMEDIATE_ACQUISITION_AND_HOLD );

		JdbcEventHandler jdbcEventHandler = Mockito.mock( JdbcEventHandler.class );
		when( sessionContext.getEventHandler() ).thenReturn( jdbcEventHandler );

		JdbcServices jdbcServices = Mockito.mock( JdbcServices.class );

		ConfigurationService configurationService = Mockito.mock( ConfigurationService.class );
		when( serviceRegistry.getService( eq( ConfigurationService.class ) ) ).thenReturn(
				configurationService );
		when( configurationService.getSetting( eq( AvailableSettings.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT ), same( StandardConverters.BOOLEAN), eq( false )) )
				.thenReturn( false );

		SqlExceptionHelper sqlExceptionHelper = Mockito.mock( SqlExceptionHelper.class );
		when( jdbcServices.getSqlExceptionHelper() ).thenReturn(
				sqlExceptionHelper );
		when(sessionOwner.getSqlExceptionHelper()).thenReturn( sqlExceptionHelper );
		JdbcCoordinatorImpl jdbcCoordinator = new JdbcCoordinatorImpl(
				null,
				sessionOwner,
				jdbcServices
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
