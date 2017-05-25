/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.internal;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.spi.PatternBasedSqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;
import org.hibernate.sql.ast.produce.sqm.spi.SqmToSqlAstConverter;
import org.hibernate.sql.ast.tree.spi.expression.Expression;

/**
 * @author Steve Ebersole
 */
public class SelfRenderingFunctionSqmExpression implements SqlAstFunctionProducer {
	private final SelfRenderingFunctionSupport renderingSupport;
	private final List<SqmExpression> sqmArguments;
	private final AllowableFunctionReturnType impliedResultType;

	public SelfRenderingFunctionSqmExpression(
			SelfRenderingFunctionSupport renderingSupport,
			List<SqmExpression> sqmArguments,
			AllowableFunctionReturnType impliedResultType) {

		this.renderingSupport = renderingSupport;
		this.sqmArguments = sqmArguments;
		this.impliedResultType = impliedResultType;
	}

	@Override
	public AllowableFunctionReturnType getExpressionType() {
		if ( getRenderingSupport().functionReturnType() != null ) {
			return getRenderingSupport().functionReturnType();
		}

		return impliedResultType;
	}

	public SelfRenderingFunctionSupport getRenderingSupport() {
		return renderingSupport;
	}

	public List<SqmExpression> getSqmArguments() {
		return sqmArguments;
	}

	@Override
	public Expression convertToSqlAst(SqmToSqlAstConverter walker) {
		return new SelfRenderingFunctionSqlAstExpression( this, walker );
	}
}
