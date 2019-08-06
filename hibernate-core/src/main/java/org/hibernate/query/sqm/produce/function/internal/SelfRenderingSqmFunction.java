/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.function.internal;

import java.util.List;

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.SqmFunction;
import org.hibernate.query.sqm.produce.function.spi.SelfRenderingFunctionSupport;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;

/**
 * @author Steve Ebersole
 */
public class SelfRenderingSqmFunction<T>
		extends AbstractSqmExpression<T>
		implements SqmFunction<T> {
	private final String name;
	private final SelfRenderingFunctionSupport renderingSupport;
	private final List<SqmTypedNode<?>> arguments;

	public SelfRenderingSqmFunction(
			SelfRenderingFunctionSupport renderingSupport,
			List<SqmTypedNode<?>> arguments,
			AllowableFunctionReturnType<T> impliedResultType,
			NodeBuilder nodeBuilder,
			String name) {
		super( impliedResultType, nodeBuilder );
		this.renderingSupport = renderingSupport;
		this.arguments = arguments;
		this.name = name;
	}

	public List<SqmTypedNode<?>> getArguments() {
		return arguments;
	}

	@Override
	public AllowableFunctionReturnType<T> getNodeType() {
		return (AllowableFunctionReturnType<T>) super.getNodeType();
	}

	public SelfRenderingFunctionSupport getRenderingSupport() {
		return renderingSupport;
	}

	@Override
	public String getFunctionName() {
		return name;
	}

}
