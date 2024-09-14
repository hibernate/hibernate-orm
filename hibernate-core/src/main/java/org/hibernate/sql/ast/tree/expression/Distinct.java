/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * @author Gavin King
 */
public class Distinct implements Expression, SqlExpressible, SqlAstNode {
	private final Expression expression;

	public Distinct(Expression expression) {
		this.expression = expression;
	}

	public Expression getExpression() {
		return expression;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		if ( expression instanceof SqlExpressible) {
			return ( (SqlExpressible) expression ).getJdbcMapping();
		}

		if ( getExpressionType() instanceof SqlExpressible) {
			return ( (SqlExpressible) getExpressionType() ).getJdbcMapping();
		}

		if ( getExpressionType() != null ) {
			assert getExpressionType().getJdbcTypeCount() == 1;
			return getExpressionType().getSingleJdbcMapping();
		}

		return null;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return expression.getExpressionType();
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitDistinct( this );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		action.accept( offset, getJdbcMapping() );
		return getJdbcTypeCount();
	}
}
