/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.sql.FakeSqmToSqlAstConverter;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Pluggable strategy for resolving a function return type for a specific call.
 *
 * @author Steve Ebersole
 */
public interface FunctionReturnTypeResolver {

	/**
	 * Resolve the return type for a function given its context-implied type and
	 * the arguments to this call.
	 * <p>
	 * The <em>context-implied</em> type is the type implied by where the function
	 * occurs in the query.  E.g., for an equality predicate (`something = some_function`)
	 * the implied type of the return from `some_function` would be defined by the type
	 * of `some_function`.
	 *
	 * @return The resolved type.
	 * @deprecated Use {@link #resolveFunctionReturnType(ReturnableType, SqmToSqlAstConverter, List, TypeConfiguration)} instead
	 */
	@Deprecated(forRemoval = true)
	default @Nullable ReturnableType<?> resolveFunctionReturnType(
			@Nullable ReturnableType<?> impliedType,
			Supplier<MappingModelExpressible<?>> inferredTypeSupplier,
			List<? extends SqmTypedNode<?>> arguments,
			TypeConfiguration typeConfiguration) {
		return resolveFunctionReturnType( impliedType, new FakeSqmToSqlAstConverter( null ) {
			@Override
			public MappingModelExpressible<?> resolveFunctionImpliedReturnType() {
				return inferredTypeSupplier.get();
			}
		}, arguments, typeConfiguration );
	}

	/**
	 * Resolve the return type for a function given its context-implied type and
	 * the arguments to this call.
	 * <p>
	 * The <em>context-implied</em> type is the type implied by where the function
	 * occurs in the query.  E.g., for an equality predicate (`something = some_function`)
	 * the implied type of the return from `some_function` would be defined by the type
	 * of `some_function`.
	 *
	 * @return The resolved type.
	 */
	default @Nullable ReturnableType<?> resolveFunctionReturnType(
			@Nullable ReturnableType<?> impliedType,
			@Nullable SqmToSqlAstConverter converter,
			List<? extends SqmTypedNode<?>> arguments,
			TypeConfiguration typeConfiguration) {
		return resolveFunctionReturnType(
				impliedType,
				converter == null ? () -> null : converter::resolveFunctionImpliedReturnType,
				arguments,
				typeConfiguration
		);
	}

	/**
	 * Resolve the return type for a function given its context-implied type and
	 * the arguments to this call.
	 * <p>
	 * The <em>context-implied</em> type is the type implied by where the function
	 * occurs in the query.  E.g., for an equality predicate (`something = some_function`)
	 * the implied type of the return from `some_function` would be defined by the type
	 * of `some_function`.
	 *
	 * @return The resolved type.
	 */
	BasicValuedMapping resolveFunctionReturnType(
			Supplier<BasicValuedMapping> impliedTypeAccess,
			List<? extends SqlAstNode> arguments);
	/**
	 * The return type in a format suitable for display to the user.
	 */
	default String getReturnType() {
		return "";
	}
}
