/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.predicate;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;

/**
 * @author Steve Ebersole
 */
public class InArrayPredicate extends AbstractPredicate {
	private final Expression testExpression;
	private final JdbcParameter arrayParameter;

	public InArrayPredicate(Expression testExpression, JdbcParameter arrayParameter, JdbcMappingContainer expressionType) {
		super( expressionType );
		this.testExpression = testExpression;
		this.arrayParameter = arrayParameter;
	}

	public InArrayPredicate(Expression testExpression, JdbcParameter arrayParameter) {
		this( testExpression, arrayParameter, null );
	}

	public Expression getTestExpression() {
		return testExpression;
	}

	public JdbcParameter getArrayParameter() {
		return arrayParameter;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitInArrayPredicate( this );
	}
}
