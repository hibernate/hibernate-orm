/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

/// Immutable semantic plan for rendering a table join.
///
/// Providers select one of these plans; the SQL AST translator owns predicate
/// traversal and SQL emission.
///
/// @since 8.0
/// @author Steve Ebersole
public sealed interface TableJoinRenderingPlan {
	/// Render the SQL AST join type and its predicate using standard syntax.
	record Standard() implements TableJoinRenderingPlan {
	}

	/// Render the joined table as a comma-separated table reference. This plan is
	/// valid only where the translator can preserve the join predicate semantics.
	record Comma() implements TableJoinRenderingPlan {
	}

	/// Render SQL Server-style `cross apply` or `outer apply` syntax.
	record Apply(Kind kind) implements TableJoinRenderingPlan {
		/// The concrete `apply` operator.
		public enum Kind {
			/// `cross apply`, corresponding to an inner lateral join.
			CROSS,
			/// `outer apply`, corresponding to a left lateral join.
			OUTER
		}
	}

	/// Report that the requested join cannot be rendered without changing its
	/// semantics. The translator raises an unsupported-operation failure.
	record Unsupported() implements TableJoinRenderingPlan {
	}
}
