/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.ast.consume.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.tree.internal.BasicValuedNonNavigableSelection;
import org.hibernate.sql.ast.tree.spi.select.Selection;

/**
 * @author Steve Ebersole
 */
public class CastFunction implements Function {
	private final Expression expressionToCast;
	private final BasicValuedExpressableType castResultType;
	private final String explicitCastTargetTypeSqlExpression;

	public CastFunction(
			Expression expressionToCast,
			BasicValuedExpressableType castResultType,
			String explicitCastTargetTypeSqlExpression) {
		this.expressionToCast = expressionToCast;
		this.castResultType = castResultType;
		this.explicitCastTargetTypeSqlExpression = explicitCastTargetTypeSqlExpression;
	}

	public Expression getExpressionToCast() {
		return expressionToCast;
	}

	public BasicValuedExpressableType getCastResultType() {
		return castResultType;
	}

	public String getExplicitCastTargetTypeSqlExpression() {
		return explicitCastTargetTypeSqlExpression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitCastFunction( this );
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return new SqlSelectionReaderImpl( castResultType );
	}

	@Override
	public Selection createSelection(Expression selectedExpression, String resultVariable) {
		return new BasicValuedNonNavigableSelection( selectedExpression, resultVariable, this );
	}

	@Override
	public ExpressableType getType() {
		return castResultType;
	}
}
