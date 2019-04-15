/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.internal;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlAstFunctionProducer;
import org.hibernate.sql.ast.produce.sqm.spi.SqmToSqlAstConverter;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public class SelfRenderingSqmFunction implements SqlAstFunctionProducer {
	private final SelfRenderingFunctionSupport renderingSupport;
	private final List<SqmExpression> sqmArguments;

	private AllowableFunctionReturnType<?> impliedResultType;

	public SelfRenderingSqmFunction(
			SelfRenderingFunctionSupport renderingSupport,
			List<SqmExpression> sqmArguments,
			AllowableFunctionReturnType<?> impliedResultType) {
		this.renderingSupport = renderingSupport;
		this.sqmArguments = sqmArguments;
		this.impliedResultType = impliedResultType;
	}

	public SelfRenderingSqmFunction(
			List<SqmExpression> sqmArguments,
			AllowableFunctionReturnType impliedResultType) {
		this.renderingSupport = null;
		this.sqmArguments = sqmArguments;
		this.impliedResultType = impliedResultType;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getExpressableType().getJavaTypeDescriptor();
	}

	@Override
	public AllowableFunctionReturnType getExpressableType() {
		return impliedResultType;
	}

	@Override
	public void applyInferableType(ExpressableType<?> type) {
		this.impliedResultType = (AllowableFunctionReturnType<?>) type;
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
