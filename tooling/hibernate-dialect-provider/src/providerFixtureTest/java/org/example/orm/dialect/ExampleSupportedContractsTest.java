/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.sql.SQLException;

import org.hibernate.dialect.temptable.spi.TemporaryTableStrategies;
import org.hibernate.jdbc.spi.JdbcExceptionHelper;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.select.SelectClause;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Exercises provider use of supported JDBC, temporary-table, and SQL AST contracts.
///
/// @author Steve Ebersole
public class ExampleSupportedContractsTest {
	@Test
	void usesTheSupportedJdbcAndTemporaryTableContracts() {
		final SQLException exception = new SQLException( "vendor", "23ABC", 57 );

		assertEquals( 57, JdbcExceptionHelper.extractErrorCode( exception ) );
		assertEquals( "23", JdbcExceptionHelper.extractSqlStateClassCode( exception ) );
		assertNotNull( TemporaryTableStrategies.sqlServerLocal() );
	}

	@Test
	void appendsAnExpressionWithoutNamingTheSelectionImplementation() {
		final Expression expression = new Expression() {
			@Override
			public JdbcMappingContainer getExpressionType() {
				return null;
			}

			@Override
			public void accept(SqlAstWalker sqlTreeWalker) {
			}
		};
		final SelectClause selectClause = new SelectClause();
		selectClause.addSqlSelection( expression );

		assertSame( expression, selectClause.getSqlSelections().get( 0 ).getExpression() );
	}
}
