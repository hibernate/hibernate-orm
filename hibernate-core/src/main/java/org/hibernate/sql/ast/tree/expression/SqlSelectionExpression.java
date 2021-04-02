/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.SqlAstWalker;

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
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> target) {
		if ( target.isInstance( this ) ) {
			return (T) this;
		}

		if ( target.isInstance( theSelection ) ) {
			return (T) theSelection;
		}

		if ( target.isInstance( theSelection.getExpression() ) ) {
			return (T) theSelection.getExpression();
		}

		if ( target.isInstance( theSelection.getExpressionType() ) ) {
			return (T) theSelection.getExpressionType();
		}

		return theSelection.getExpression().unwrap( target );
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitSqlSelectionExpression( this );
	}

	@Override
	public MappingModelExpressable getExpressionType() {
		return theSelection.getExpressionType();
	}
}
