/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.ast.spi;

import org.hibernate.sql.ast.spi.AbstractSqlAstWalker;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Verifies structural traversal behavior exposed to direct SQL AST walkers.
///
/// @author Steve Ebersole
public class AbstractSqlAstWalkerTests {
	@Test
	void traversesThroughASelectionReferenceToItsExpression() {
		final SqlSelection selection = mock( SqlSelection.class );
		final Expression expression = mock( Expression.class );
		final AbstractSqlAstWalker walker = new AbstractSqlAstWalker();
		when( selection.getExpression() ).thenReturn( expression );

		walker.visitSqlSelectionExpression( new SqlSelectionExpression( selection ) );

		verify( expression ).accept( walker );
	}
}
