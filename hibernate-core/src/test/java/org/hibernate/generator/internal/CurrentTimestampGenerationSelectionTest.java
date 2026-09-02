/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.ResultSetReturn;
import org.hibernate.engine.jdbc.spi.StatementPreparer;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.resource.jdbc.ResourceRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Verifies callable current-timestamp selection independently of any
/// production Dialect command.
///
/// @author Steve Ebersole
public class CurrentTimestampGenerationSelectionTest {
	@Test
	void executesPreparedSelectionUsingTheFirstResultColumn() throws Exception {
		final String command = "select fixture_now()";
		final Timestamp expected = Timestamp.valueOf( "2026-08-26 12:34:56" );
		final var session = mock( SharedSessionContractImplementor.class );
		final var coordinator = mock( JdbcCoordinator.class );
		final var preparer = mock( StatementPreparer.class );
		final var statement = mock( PreparedStatement.class );
		final var resultSet = mock( ResultSet.class );
		final var resultSetReturn = mock( ResultSetReturn.class );
		final var logicalConnection = mock( LogicalConnectionImplementor.class );
		final var resourceRegistry = mock( ResourceRegistry.class );

		when( session.getJdbcCoordinator() ).thenReturn( coordinator );
		when( coordinator.getStatementPreparer() ).thenReturn( preparer );
		when( preparer.prepareStatement( command, false ) ).thenReturn( statement );
		when( coordinator.getResultSetReturn() ).thenReturn( resultSetReturn );
		when( resultSetReturn.extract( statement, command ) ).thenReturn( resultSet );
		when( resultSet.getTimestamp( 1 ) ).thenReturn( expected );
		when( coordinator.getLogicalConnection() ).thenReturn( logicalConnection );
		when( logicalConnection.getResourceRegistry() ).thenReturn( resourceRegistry );

		assertSame( expected, CurrentTimestampGeneration.getCurrentTimestampFromDatabase( command, false, session ) );
		verify( resultSet ).next();
		verify( resourceRegistry ).release( statement );
		verify( coordinator ).afterStatementExecution();
	}

	@Test
	void executesCallableSelectionUsingTheFirstOutParameter() throws Exception {
		final String command = "{?=call fixture_now()}";
		final Timestamp expected = Timestamp.valueOf( "2026-08-26 12:34:56" );
		final var session = mock( SharedSessionContractImplementor.class );
		final var coordinator = mock( JdbcCoordinator.class );
		final var preparer = mock( StatementPreparer.class );
		final var statement = mock( CallableStatement.class );
		final var resultSetReturn = mock( ResultSetReturn.class );
		final var logicalConnection = mock( LogicalConnectionImplementor.class );
		final var resourceRegistry = mock( ResourceRegistry.class );

		when( session.getJdbcCoordinator() ).thenReturn( coordinator );
		when( coordinator.getStatementPreparer() ).thenReturn( preparer );
		when( preparer.prepareStatement( command, true ) ).thenReturn( statement );
		when( coordinator.getResultSetReturn() ).thenReturn( resultSetReturn );
		when( statement.getTimestamp( 1 ) ).thenReturn( expected );
		when( coordinator.getLogicalConnection() ).thenReturn( logicalConnection );
		when( logicalConnection.getResourceRegistry() ).thenReturn( resourceRegistry );

		assertSame( expected, CurrentTimestampGeneration.getCurrentTimestampFromDatabase( command, true, session ) );
		verify( statement ).registerOutParameter( 1, Types.TIMESTAMP );
		verify( resultSetReturn ).execute( statement, command );
		verify( resourceRegistry ).release( statement );
		verify( coordinator ).afterStatementExecution();
	}

	@Test
	void rejectsDatabaseSelectionWhenTheDialectSuppliesNoCommand() {
		final var session = mock( SharedSessionContractImplementor.class );
		final var jdbcServices = mock( JdbcServices.class );
		final var jdbcEnvironment = mock( JdbcEnvironment.class );
		final var dialect = new Dialect( DatabaseVersion.make( 1 ) ) {};
		when( session.getJdbcServices() ).thenReturn( jdbcServices );
		when( jdbcServices.getJdbcEnvironment() ).thenReturn( jdbcEnvironment );
		when( jdbcEnvironment.getDialect() ).thenReturn( dialect );

		assertThrows( UnsupportedOperationException.class, () -> CurrentTimestampGeneration.getCurrentTimestamp( session ) );
	}
}
