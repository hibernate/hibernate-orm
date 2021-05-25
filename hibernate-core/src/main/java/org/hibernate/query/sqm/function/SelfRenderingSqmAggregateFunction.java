/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;

/**
 * @author Christian Beikov
 */
public class SelfRenderingSqmAggregateFunction<T> extends SelfRenderingSqmFunction<T> {

	private final SqmPredicate filter;

	public SelfRenderingSqmAggregateFunction(
			SqmFunctionDescriptor descriptor,
			FunctionRenderingSupport renderingSupport,
			List<SqmTypedNode<?>> arguments,
			SqmPredicate filter,
			AllowableFunctionReturnType<T> impliedResultType,
			FunctionReturnTypeResolver returnTypeResolver,
			NodeBuilder nodeBuilder,
			String name) {
		super( descriptor, renderingSupport, arguments, impliedResultType, returnTypeResolver, nodeBuilder, name );
		this.filter = filter;
	}

	@Override
	public SelfRenderingFunctionSqlAstExpression convertToSqlAst(SqmToSqlAstConverter walker) {
		final AllowableFunctionReturnType<?> resultType = resolveResultType(
				walker.getCreationContext().getDomainModel().getTypeConfiguration()
		);

		return new SelfRenderingAggregateFunctionSqlAstExpression(
				getFunctionName(),
				getRenderingSupport(),
				resolveSqlAstArguments( getArguments(), walker ),
				filter == null ? null : (Predicate) filter.accept( walker ),
				resultType,
				getMappingModelExpressable( walker, resultType )
		);
	}
}
