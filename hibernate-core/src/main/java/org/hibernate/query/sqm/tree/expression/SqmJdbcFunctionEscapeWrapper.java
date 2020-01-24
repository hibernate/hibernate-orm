/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.function.SelfRenderingSqlFunctionExpression;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;

/**
 * Adds a JDBC function escape (i.e. `{fn <wrapped-function-call>})  around the wrapped function
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class SqmJdbcFunctionEscapeWrapper<T>
		extends SelfRenderingSqlFunctionExpression<T> {
	private final SelfRenderingSqlFunctionExpression<?> wrappedSqmFunction;

	public SqmJdbcFunctionEscapeWrapper(
			SqmFunctionDescriptor fun,
			SelfRenderingSqlFunctionExpression<T> wrappedSqmFunction,
			NodeBuilder nodeBuilder) {
		super(
				fun,
				(sqlAppender, sqlAstArguments, walker) -> {
					sqlAppender.appendSql( "{fn " );
					wrappedSqmFunction.getRenderingSupport().render(
							sqlAppender,
							sqlAstArguments,
							walker
					);
					sqlAppender.appendSql( "}" );
				},
				wrappedSqmFunction.getArguments(),
				(AllowableFunctionReturnType<T>) wrappedSqmFunction.getNodeType(),
				null, //TODO!!!!!!
				nodeBuilder,
				wrappedSqmFunction.getFunctionName()
		);
		this.wrappedSqmFunction = wrappedSqmFunction;
	}

	@Override
	public String asLoggableText() {
		return "wrapped-function[ " + wrappedSqmFunction.asLoggableText() + " ]";
	}

}
