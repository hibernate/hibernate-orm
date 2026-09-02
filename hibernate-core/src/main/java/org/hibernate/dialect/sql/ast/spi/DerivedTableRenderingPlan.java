/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;

import jakarta.annotation.Nullable;

/// Immutable plan selected by [DerivedTableRenderingSupport] for rendering a
/// derived-table reference.
///
/// The selected subtype must match [DerivedTableRenderingRequest#kind]. The
/// translator interprets the plan and retains ownership of all mutable state.
///
/// @since 8.0
/// @author Steve Ebersole
public sealed interface DerivedTableRenderingPlan {
	/// How the derived table's column aliases are rendered.
	DerivedColumnAliasing columnAliasing();

	/// How references to enclosing query roots are expressed.
	LateralReferenceStyle lateralReferenceStyle();

	/// An optional rendering mode applied while rendering contained SQL AST
	/// expressions.
	@Nullable SqlAstNodeRenderingMode renderingMode();

	/// Rendering choices for a derived query part.
	///
	/// @param tablePrefix whether to prefix the subquery with the `table` keyword
	record QueryPart(
			DerivedColumnAliasing columnAliasing,
			LateralReferenceStyle lateralReferenceStyle,
			boolean tablePrefix,
			@Nullable SqlAstNodeRenderingMode renderingMode) implements DerivedTableRenderingPlan {
	}

	/// Rendering choices for a derived values table.
	record Values(
			DerivedColumnAliasing columnAliasing,
			ValuesTableRenderingStyle renderingStyle,
			@Nullable SqlAstNodeRenderingMode renderingMode) implements DerivedTableRenderingPlan {
		@Override
		public LateralReferenceStyle lateralReferenceStyle() {
			return LateralReferenceStyle.IMPLICIT;
		}
	}

	/// Rendering choices for a function table reference.
	record Function(
			DerivedColumnAliasing columnAliasing,
			LateralReferenceStyle lateralReferenceStyle,
			@Nullable SqlAstNodeRenderingMode renderingMode) implements DerivedTableRenderingPlan {
	}
}
