/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleType;
import org.hibernate.query.sqm.produce.function.internal.SetReturningFunctionTypeResolverBuilder;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * Pluggable strategy for resolving a function return type for a specific call.
 *
 * @since 7.0
 */
@Incubating
public interface SetReturningFunctionTypeResolver {

	/**
	 * Resolve the return type for a function given its arguments to this call.
	 *
	 * @return The resolved type.
	 */
	AnonymousTupleType<?> resolveTupleType(List<? extends SqmTypedNode<?>> arguments, TypeConfiguration typeConfiguration);

	/**
	 * Resolve the tuple elements {@link SqlExpressible} for a function given its arguments to this call.
	 *
	 * @return The resolved JdbcMapping.
	 */
	SelectableMapping[] resolveFunctionReturnType(
			List<? extends SqlAstNode> arguments,
			String tableIdentifierVariable,
			boolean lateral,
			boolean withOrdinality,
			SqmToSqlAstConverter converter);

	/**
	 * Creates a builder for a type resolver.
	 */
	static Builder builder() {
		return new SetReturningFunctionTypeResolverBuilder();
	}

	/**
	 * Pluggable strategy for resolving a function return type for a specific call.
	 *
	 * @since 7.0
	 */
	@Incubating
	interface Builder {

		/**
		 * Like {@link #invariant(String, BasicTypeReference, String)}, but passing the component as selection expression.
		 *
		 * @see #invariant(String, BasicTypeReference, String)
		 */
		Builder invariant(String component, BasicTypeReference<?> invariantType);

		/**
		 * Specifies that the return type has a component with the given name being selectable through the given
		 * selection expression, which has the given invariant type.
		 */
		Builder invariant(String component, BasicTypeReference<?> invariantType, String selectionExpression);

		/**
		 * Like {@link #invariant(String, BasicType, String)}, but passing the component as selection expression.
		 *
		 * @see #invariant(String, BasicType, String)
		 */
		Builder invariant(String component, BasicType<?> invariantType);

		/**
		 * Specifies that the return type has a component with the given name being selectable through the given
		 * selection expression, which has the given invariant type.
		 */
		Builder invariant(String component, BasicType<?> invariantType, String selectionExpression);

		/**
		 * Like {@link #useArgType(String, int, String)}, but passing the component as selection expression.
		 *
		 * @see #useArgType(String, int, String)
		 */
		Builder useArgType(String component, int argPosition);

		/**
		 * Specifies that the return type has a component with the given name being selectable through the given
		 * selection expression, which has the same type as the argument of the given 0-based position.
		 */
		Builder useArgType(String component, int argPosition, String selectionExpression);

		/**
		 * Builds a type resolver.
		 */
		SetReturningFunctionTypeResolver build();
	}
}
