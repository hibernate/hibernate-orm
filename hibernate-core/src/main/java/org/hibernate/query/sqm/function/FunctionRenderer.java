/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import java.util.List;

import org.hibernate.SPI;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstNode;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.select.SortSpecification;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/// Renders the SQL AST representation of a self-rendering
/// [SqmFunctionDescriptor].
///
/// Implement this contract as part of a custom self-rendering descriptor. It
/// is passed through [SelfRenderingSqmFunction] to
/// [SelfRenderingFunctionSqlAstExpression] and is not supplied independently
/// to Hibernate.
///
/// @author Steve Ebersole
/// @since 6.4
@SPI({ USE, IMPLEMENT })
@FunctionalInterface
public interface FunctionRenderer {
	/**
	 * @deprecated Use {@link #render(SqlAppender, List, ReturnableType, SqlAstTranslator)} instead
	 */
	@Deprecated(forRemoval = true)
	default void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			SqlAstTranslator<?> walker) {
		render( sqlAppender, sqlAstArguments, null, walker );
	}

	void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker);

	default void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// Ignore the filter by default. Subclasses will override this
		render( sqlAppender, sqlAstArguments, returnType, walker );
	}

	default void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			List<SortSpecification> withinGroup,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// Ignore the filter by default. Subclasses will override this
		render( sqlAppender, sqlAstArguments, returnType, walker );
	}

	default void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			Predicate filter,
			Boolean respectNulls,
			Boolean fromFirst,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		// Ignore the filter by default. Subclasses will override this
		render( sqlAppender, sqlAstArguments, returnType, walker );
	}

}
