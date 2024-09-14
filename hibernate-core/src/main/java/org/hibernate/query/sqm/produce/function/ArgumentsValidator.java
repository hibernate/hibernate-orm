/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.produce.function;

import org.hibernate.query.spi.QueryEngine;
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
	 * @deprecated Use {@link #validate(List, String, TypeConfiguration)}
	 */
	@Deprecated(since = "6.2")
	default void validate(List<? extends SqmTypedNode<?>> arguments, String functionName, QueryEngine queryEngine) {
		validate( arguments, functionName, queryEngine.getTypeConfiguration() );
	}

	/**
	 * Perform validation that may be done using the {@link SqmTypedNode} tree and assigned Java types.
	 */
	default void validate(List<? extends SqmTypedNode<?>> arguments, String functionName, TypeConfiguration typeConfiguration) {}

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
