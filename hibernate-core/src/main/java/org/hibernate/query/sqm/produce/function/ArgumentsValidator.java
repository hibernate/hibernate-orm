/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.type.BindingContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * Validates the arguments provided to an HQL function invocation.
 *
 * @author Steve Ebersole
 *
 * @see StandardArgumentsValidators
 * @see ArgumentTypesValidator
 */
public interface ArgumentsValidator {

	/**
	 * Perform validation that may be done using the {@link SqmTypedNode} tree and assigned Java types.
	 *
	 * @since 7.0
	 */
	default void validate(List<? extends SqmTypedNode<?>> arguments, String functionName, BindingContext bindingContext) {
		validate( arguments, functionName, bindingContext.getTypeConfiguration() );
	}

	/**
	 * Perform validation that may be done using the {@link SqmTypedNode} tree and assigned Java types.
	 *
	 * @deprecated Implement {@link #validate(List, String, BindingContext)} instead
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	default void validate(List<? extends SqmTypedNode<?>> arguments, String functionName, TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException( "Deprecated operation not implemented" );
	}

	/**
	 * Pretty-print the signature of the argument list.
	 */
	default String getSignature() {
		return "( ... )";
	}

	/**
	 * Perform validation that requires the {@link SqlAstNode} tree and assigned JDBC types.
	 */
	default void validateSqlTypes(List<? extends SqlAstNode> arguments, String functionName) {}

}
