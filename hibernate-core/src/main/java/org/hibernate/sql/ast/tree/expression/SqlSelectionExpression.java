/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlSelection;

/**
 * Represents a selection that is "re-used" in certain parts of the query
 * other than the select-clause (mainly important for order-by, group-by and
 * having).  Allows usage of the selection position within the select-clause
 * in that other part of the query rather than the full expression
 *
 * @author Steve Ebersole
 */
public class SqlSelectionExpression implements Expression {
	private final SqlSelection theSelection;

	public SqlSelectionExpression(SqlSelection theSelection) {
		this.theSelection = theSelection;
	}

	public SqlSelection getSelection() {
		return theSelection;
	}

	@Override
	public ColumnReference getColumnReference() {
		return theSelection.getExpression().getColumnReference();
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitSqlSelectionExpression( this );
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return theSelection.getExpressionType();
	}
}
