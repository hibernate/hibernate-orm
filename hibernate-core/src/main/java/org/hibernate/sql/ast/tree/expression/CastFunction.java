/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public class CastFunction extends AbstractFunction {
	private final Expression expressionToCast;
	private final CastTarget castTarget;

	public CastFunction(Expression expressionToCast, CastTarget castTarget) {
		this.expressionToCast = expressionToCast;
		this.castTarget = castTarget;
	}

	public Expression getExpressionToCast() {
		return expressionToCast;
	}

	public CastTarget getCastTarget() {
		return castTarget;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return getType();
	}

	@Override
	public SqlExpressableType getType() {
		return castTarget.getExpressableType();
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitCastFunction( this );
	}
}
