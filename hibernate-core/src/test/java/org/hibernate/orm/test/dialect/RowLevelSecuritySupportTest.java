/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurity;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdl;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdlRequest;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityStrategies;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;

import org.junit.jupiter.api.Test;

import static org.hibernate.dialect.rowsecurity.spi.RowLevelSecurityDdl.Phase.AFTER_TABLES;
import static org.hibernate.dialect.rowsecurity.spi.TenantIdentifierSource.DATABASE_USER;
import static org.hibernate.dialect.rowsecurity.spi.TenantIdentifierSource.SESSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/// Verifies the supported row-level-security provider boundary.
///
/// @author Steve Ebersole
/// @since 8.0
public class RowLevelSecuritySupportTest {
	@Test
	void unsupportedStrategyIsStableAndInert() throws SQLException {
		final RowLevelSecurity none = RowLevelSecurityStrategies.none();
		assertSame( none, RowLevelSecurityStrategies.none() );
		assertFalse( none.supportsRowLevelSecurity() );
		assertFalse( none.supportsTenantIdentifierSource( SESSION ) );
		assertFalse( none.supportsTenantIdentifierSource( DATABASE_USER ) );
		assertTrue( none.getTenantTableDdl( mock( RowLevelSecurityDdlRequest.class ) ).isEmpty() );

		final Connection connection = mock( Connection.class );
		none.setTenantIdentifier( connection, "tenant", true );
		verifyNoInteractions( connection );
	}

	@Test
	void ddlResultsAreValidatedAndImmutable() {
		final var creates = new ArrayList<>( List.of( "create policy" ) );
		final var descriptor = new RowLevelSecurityDdl(
				"policy",
				AFTER_TABLES,
				creates,
				List.of( "drop policy" ),
				Set.of( Dialect.class.getName() )
		);
		creates.clear();
		assertEquals( List.of( "create policy" ), descriptor.createCommands() );
		assertThrows( UnsupportedOperationException.class, () -> descriptor.createCommands().clear() );
		assertThrows(
				IllegalArgumentException.class,
				() -> new RowLevelSecurityDdl( " ", AFTER_TABLES, List.of( "create" ), List.of(), Set.of() )
		);
		assertThrows(
				IllegalArgumentException.class,
				() -> new RowLevelSecurityDdl( "empty", AFTER_TABLES, List.of(), List.of(), Set.of() )
		);
		assertThrows(
				IllegalArgumentException.class,
				() -> new RowLevelSecurityDdl( "phase", null, List.of( "create" ), List.of(), Set.of() )
		);
	}

	@Test
	void maintainedDialectProfilesRetainVersionGates() {
		for ( var support : List.of(
				new DB2Dialect().getRowLevelSecurity(),
				new PostgreSQLDialect().getRowLevelSecurity(),
				new CockroachDialect( DatabaseVersion.make( 25, 2 ) ).getRowLevelSecurity(),
				new SQLServerDialect( DatabaseVersion.make( 13 ) ).getRowLevelSecurity() ) ) {
			assertTrue( support.supportsRowLevelSecurity() );
			assertTrue( support.getClass().getPackageName().endsWith( ".rowsecurity.internal" ) );
		}
		assertSame(
				RowLevelSecurityStrategies.none(),
				new CockroachDialect( DatabaseVersion.make( 25, 1 ) ).getRowLevelSecurity()
		);
		assertSame(
				RowLevelSecurityStrategies.none(),
				new SQLServerDialect( DatabaseVersion.make( 12 ) ).getRowLevelSecurity()
		);
		assertSame( RowLevelSecurityStrategies.none(), new SpannerPostgreSQLDialect().getRowLevelSecurity() );
	}

	@Test
	void maintainedConnectionOperationsForwardParameters() throws SQLException {
		final Connection postgresConnection = mock( Connection.class );
		final PreparedStatement postgresStatement = mock( PreparedStatement.class );
		when( postgresConnection.prepareStatement(
				"select set_config('hibernate.tenant_id', ?, true), set_config('hibernate.tenant_id_root', ?, true)"
		) ).thenReturn( postgresStatement );
		new PostgreSQLDialect().getRowLevelSecurity()
				.setTenantIdentifier( postgresConnection, "acme", true );
		verify( postgresStatement ).setString( 1, "acme" );
		verify( postgresStatement ).setString( 2, "true" );
		verify( postgresStatement ).execute();

		final Connection cockroachConnection = mock( Connection.class );
		final PreparedStatement cockroachStatement = mock( PreparedStatement.class );
		when( cockroachConnection.prepareStatement( "set application_name = ?" ) ).thenReturn( cockroachStatement );
		new CockroachDialect( DatabaseVersion.make( 25, 2 ) ).getRowLevelSecurity()
				.setTenantIdentifier( cockroachConnection, "acme", false );
		verify( cockroachStatement ).setString( 1, "hibernate_orm_rls:false:acme" );
		verify( cockroachStatement ).execute();

		assertTwoStatementOperation(
				new DB2Dialect().getRowLevelSecurity(),
				"set hibernate.tenant_id = ?",
				"set hibernate.tenant_id_root = ?"
		);
		assertTwoStatementOperation(
				new SQLServerDialect( DatabaseVersion.make( 13 ) ).getRowLevelSecurity(),
				"exec sys.sp_set_session_context @key=N'hibernate.tenant_id', @value=?",
				"exec sys.sp_set_session_context @key=N'hibernate.tenant_id_root', @value=?"
		);
	}

	private static void assertTwoStatementOperation(
			RowLevelSecurity support,
			String tenantSql,
			String rootSql) throws SQLException {
		final Connection connection = mock( Connection.class );
		final PreparedStatement tenantStatement = mock( PreparedStatement.class );
		final PreparedStatement rootStatement = mock( PreparedStatement.class );
		when( connection.prepareStatement( tenantSql ) ).thenReturn( tenantStatement );
		when( connection.prepareStatement( rootSql ) ).thenReturn( rootStatement );
		support.setTenantIdentifier( connection, "acme", true );
		verify( tenantStatement ).setString( 1, "acme" );
		verify( tenantStatement ).execute();
		verify( rootStatement ).setInt( 1, 1 );
		verify( rootStatement ).execute();
	}

	@Test
	void sqlExceptionsPropagateUnchanged() throws SQLException {
		final SQLException failure = new SQLException( "connection failed" );
		final Connection connection = mock( Connection.class );
		when( connection.prepareStatement( "set application_name = ?" ) ).thenThrow( failure );
		final SQLException thrown = assertThrows(
				SQLException.class,
				() -> new CockroachDialect( DatabaseVersion.make( 25, 2 ) ).getRowLevelSecurity()
						.setTenantIdentifier( connection, "acme", false )
		);
		assertSame( failure, thrown );
	}

	@Test
	void supportedSignaturesExcludeBootAndRenderingTypes() {
		final Set<Class<?>> forbidden = Set.of(
				Table.class,
				Column.class,
				Metadata.class,
				InFlightMetadataCollector.class,
				Database.class,
				AuxiliaryDatabaseObject.class,
				SqlStringGenerationContext.class
		);
		for ( Class<?> contract : List.of( RowLevelSecurity.class, RowLevelSecurityDdlRequest.class ) ) {
			for ( var method : contract.getDeclaredMethods() ) {
				assertFalse( forbidden.contains( method.getReturnType() ), method::toString );
				for ( var parameterType : method.getParameterTypes() ) {
					assertFalse( forbidden.contains( parameterType ), method::toString );
				}
			}
		}
	}
}
