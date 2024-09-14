/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.predicate;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.select.QueryPart;
import org.hibernate.sql.ast.tree.select.SelectStatement;

/**
 * @author Gavin King
 */
public class ExistsPredicate implements Predicate {

	private final boolean negated;
	private final SelectStatement expression;
	private final JdbcMappingContainer expressionType;

	public ExistsPredicate(QueryPart expression, boolean negated, JdbcMappingContainer expressionType) {
		this( new SelectStatement( expression ), negated, expressionType );
	}

	public ExistsPredicate(SelectStatement expression, boolean negated, JdbcMappingContainer expressionType) {
		this.negated = negated;
		this.expression = expression;
		this.expressionType = expressionType;
	}

	public SelectStatement getExpression() {
		return expression;
	}

	public boolean isNegated() {
		return negated;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitExistsPredicate( this );
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return expressionType;
	}
}
