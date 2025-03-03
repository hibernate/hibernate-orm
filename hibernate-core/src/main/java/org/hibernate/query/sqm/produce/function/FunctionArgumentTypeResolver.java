/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.produce.function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.sql.ast.tree.expression.Expression;

import java.util.List;

/**
 * Pluggable strategy for resolving a function argument type for a specific call.
 *
 * @author Christian Beikov
 */
public interface FunctionArgumentTypeResolver {
	/**
	 * Resolve the argument type for a function given its context-implied return type.
	 * <p>
	 * The <em>context-implied</em> type is the type implied by where the function
	 * occurs in the query.  E.g., for an equality predicate (`something = some_function`)
	 * the implied type would be defined by the type of `something`.
	 *
	 * @return The resolved type.
	 * @deprecated Use {@link #resolveFunctionArgumentType(List, int, SqmToSqlAstConverter)} instead
	 */
	@Deprecated(forRemoval = true)
	@Nullable MappingModelExpressible<?> resolveFunctionArgumentType(
			SqmFunction<?> function,
			int argumentIndex,
			SqmToSqlAstConverter converter);

	/**
	 * Resolve the argument type for a function given its context-implied return type.
	 * <p>
	 * The <em>context-implied</em> type is the type implied by where the function
	 * occurs in the query.  E.g., for an equality predicate (`something = some_function`)
	 * the implied type would be defined by the type of `something`.
	 *
	 * @return The resolved type.
	 * @since 7.0
	 */
	default @Nullable MappingModelExpressible<?> resolveFunctionArgumentType(
			List<? extends SqmTypedNode<?>> arguments,
			int argumentIndex,
			SqmToSqlAstConverter converter) {
		return resolveFunctionArgumentType(
				new SqmFunction<>(
						"",
						new NamedSqmFunctionDescriptor( "", false, null, null ),
						null,
						arguments,
						converter.getSqmCreationContext().getNodeBuilder()
				) {
					@Override
					public Expression convertToSqlAst(SqmToSqlAstConverter walker) {
						throw new UnsupportedOperationException();
					}

					@Override
					public SqmExpression<Object> copy(SqmCopyContext context) {
						throw new UnsupportedOperationException();
					}
				},
				argumentIndex,
				converter
		);
	}
}
