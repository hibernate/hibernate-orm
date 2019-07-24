/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.function.internal.SelfRenderingSqmFunction;

/**
 * Adds a JDBC function escape (i.e. `{fn <wrapped-function-call>})  around the wrapped function
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public class SqmJdbcFunctionEscapeWrapper<T>
		extends SelfRenderingSqmFunction<T> {
	private final SelfRenderingSqmFunction<?> wrappedSqmFunction;

	public SqmJdbcFunctionEscapeWrapper(
			SelfRenderingSqmFunction<T> wrappedSqmFunction,
			NodeBuilder nodeBuilder) {
		super(
				(sqlAppender,
				sqlAstArguments,
				walker,
				sessionFactory) -> {
					sqlAppender.appendSql( "{fn " );
					wrappedSqmFunction.getRenderingSupport().render(
							sqlAppender,
							sqlAstArguments,
							walker,
							sessionFactory
					);
					sqlAppender.appendSql( "}" );
				},
				wrappedSqmFunction.getArguments(),
				wrappedSqmFunction.getNodeType(),
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
