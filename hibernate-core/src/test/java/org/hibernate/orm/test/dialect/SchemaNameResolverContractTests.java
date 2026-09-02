/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SybaseDialect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Verifies connection-based and query-based schema-name resolution contracts.
///
/// @author Steve Ebersole
public class SchemaNameResolverContractTests {
	@Test
	void defaultResolverPreservesConnectionResults() throws SQLException {
		final Connection connection = mock( Connection.class );
		when( connection.getSchema() ).thenReturn( "orm", "", null );
		final var dialect = new H2Dialect();
		final var resolver = dialect.getSchemaNameResolver();

		assertThat( resolver.resolveSchemaName( connection, dialect ) ).isEqualTo( "orm" );
		assertThat( resolver.resolveSchemaName( connection, dialect ) ).isEmpty();
		assertThat( resolver.resolveSchemaName( connection, dialect ) ).isNull();
	}

	@Test
	void sybaseResolverOwnsExactQueryAndClosesItsResources() throws SQLException {
		final Connection connection = mock( Connection.class );
		final Statement statement = mock( Statement.class );
		final ResultSet resultSet = mock( ResultSet.class );
		when( connection.createStatement() ).thenReturn( statement );
		when( statement.executeQuery( "select user_name()" ) ).thenReturn( resultSet );
		when( resultSet.next() ).thenReturn( true );
		when( resultSet.getString( 1 ) ).thenReturn( "dbo" );
		final var dialect = new SybaseDialect();

		assertThat( dialect.getSchemaNameResolver().resolveSchemaName( connection, dialect ) )
				.isEqualTo( "dbo" );
		verify( resultSet ).close();
		verify( statement ).close();
	}

	@Test
	void sybaseResolverReturnsNullForNoRowAndPropagatesSqlExceptions() throws SQLException {
		final Connection noRowConnection = mock( Connection.class );
		final Statement noRowStatement = mock( Statement.class );
		final ResultSet noRowResultSet = mock( ResultSet.class );
		when( noRowConnection.createStatement() ).thenReturn( noRowStatement );
		when( noRowStatement.executeQuery( "select user_name()" ) ).thenReturn( noRowResultSet );
		final var dialect = new SybaseDialect();
		assertThat( dialect.getSchemaNameResolver().resolveSchemaName( noRowConnection, dialect ) ).isNull();

		final Connection failingConnection = mock( Connection.class );
		when( failingConnection.createStatement() ).thenThrow( new SQLException( "schema failure" ) );
		assertThatExceptionOfType( SQLException.class )
				.isThrownBy( () -> dialect.getSchemaNameResolver().resolveSchemaName( failingConnection, dialect ) )
				.withMessage( "schema failure" );
	}

	@Test
	void spannerPreservesItsDeliberateEmptySchema() throws SQLException {
		final var dialect = new SpannerDialect();
		assertThat( dialect.getSchemaNameResolver().resolveSchemaName( mock( Connection.class ), dialect ) )
				.isEmpty();
	}
}
