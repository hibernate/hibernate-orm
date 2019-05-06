/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.internal;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.sql.ast.produce.spi.SqmFunction;
import org.hibernate.sql.ast.produce.sqm.spi.SqmToSqlAstConverter;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public class SelfRenderingSqmFunction<T> extends AbstractSqmExpression<T> implements SqmFunction<T> {
	private final SelfRenderingFunctionSupport renderingSupport;
	private List<SqmTypedNode<?>> arguments;

	public SelfRenderingSqmFunction(
			SelfRenderingFunctionSupport renderingSupport,
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			NodeBuilder nodeBuilder) {
		super( impliedResultType, nodeBuilder );
		this.renderingSupport = renderingSupport;
		this.arguments = arguments;
	}

	public List<SqmTypedNode<?>> getArguments() {
		return arguments;
	}

	@Override
	public AllowableFunctionReturnType<T> getExpressableType() {
		return (AllowableFunctionReturnType<T>) super.getExpressableType();
	}

	public SelfRenderingFunctionSupport getRenderingSupport() {
		return renderingSupport;
	}

	@Override
	public SelfRenderingFunctionSqlAstExpression<T> convertToSqlAst(SqmToSqlAstConverter walker) {
		return new SelfRenderingFunctionSqlAstExpression<>( this, walker );
	}

	@Override
	public String getFunctionName() {
		//TODO
		return null;
	}

	@Override
	public boolean isAggregator() {
		//TODO
		return false;
	}

}
