/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.predicate;

import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * @author Gavin King
 */
public class ExistsPredicate implements Predicate {

	private Expression expression;

	public ExistsPredicate(Expression expression) {
		this.expression = expression;
	}

	public Expression getExpression() {
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
}
