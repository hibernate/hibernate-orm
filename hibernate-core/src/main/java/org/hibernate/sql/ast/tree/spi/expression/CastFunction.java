/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public class CastFunction extends AbstractStandardFunction {
	private final Expression expressionToCast;
	private final SqlExpressableType castResultType;
	private final String explicitCastTargetTypeSqlExpression;

	public CastFunction(
			Expression expressionToCast,
			SqlExpressableType castResultType,
			String explicitCastTargetTypeSqlExpression) {
		this.expressionToCast = expressionToCast;
		this.castResultType = castResultType;
		this.explicitCastTargetTypeSqlExpression = explicitCastTargetTypeSqlExpression;
	}

	public Expression getExpressionToCast() {
		return expressionToCast;
	}

	public SqlExpressableType getCastResultType() {
		return castResultType;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return getCastResultType();
	}

	@Override
	public SqlExpressableType getType() {
		return getCastResultType();
	}

	public String getExplicitCastTargetTypeSqlExpression() {
		return explicitCastTargetTypeSqlExpression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitCastFunction( this );
	}
}
