/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.predicate;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.select.QueryPart;

/**
 * @author Gavin King
 */
public class ExistsPredicate implements Predicate {

	private final QueryPart expression;
	private final JdbcMappingContainer expressionType;

	public ExistsPredicate(QueryPart expression, JdbcMappingContainer expressionType) {
		this.expression = expression;
		this.expressionType = expressionType;
	}

	public QueryPart getExpression() {
		return expression;
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
