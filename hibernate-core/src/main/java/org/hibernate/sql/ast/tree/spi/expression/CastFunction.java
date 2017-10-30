/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * @author Steve Ebersole
 */
public class CastFunction extends AbstractStandardFunction {
	private final Expression expressionToCast;
	private final AllowableFunctionReturnType castResultType;
	private final String explicitCastTargetTypeSqlExpression;

	public CastFunction(
			Expression expressionToCast,
			AllowableFunctionReturnType castResultType,
			String explicitCastTargetTypeSqlExpression) {
		this.expressionToCast = expressionToCast;
		this.castResultType = castResultType;
		this.explicitCastTargetTypeSqlExpression = explicitCastTargetTypeSqlExpression;
	}

	public Expression getExpressionToCast() {
		return expressionToCast;
	}

	public AllowableFunctionReturnType getCastResultType() {
		return castResultType;
	}

	@Override
	public AllowableFunctionReturnType getType() {
		return getCastResultType();
	}

	public String getExplicitCastTargetTypeSqlExpression() {
		return explicitCastTargetTypeSqlExpression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitCastFunction( this );
	}

	@Override
	public SqlSelection createSqlSelection(int jdbcPosition) {
		return new SqlSelectionImpl(
				jdbcPosition,
				this,
				( (BasicValuedExpressableType) getType() ).getBasicType().getSqlSelectionReader()
		);
	}
}
