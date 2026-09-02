/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.ast;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.select.SelectClause;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Tests construction of SQL AST select clauses.
///
/// @author Steve Ebersole
public class SelectClauseTests {
	@Test
	void addsAnExpressionWithDefaultPositionsAndAcceptsAnExistingSelection() {
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
		final SqlSelection generated = selectClause.getSqlSelections().get( 0 );
		assertThat( generated.getExpression() ).isSameAs( expression );
		assertThat( generated.getJdbcResultSetIndex() ).isZero();
		assertThat( generated.getValuesArrayPosition() ).isEqualTo( -1 );

		selectClause.addSqlSelection( generated );
		assertThat( selectClause.getSqlSelections() ).containsExactly( generated, generated );
	}
}
