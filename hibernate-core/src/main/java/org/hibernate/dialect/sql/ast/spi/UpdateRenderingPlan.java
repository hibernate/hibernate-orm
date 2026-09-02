/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import jakarta.annotation.Nullable;

/// Immutable semantic plan for rendering a query update.
///
/// Providers select the syntax or emulation family. The translator owns SQL AST
/// traversal, mutation predicates, assignments, and SQL emission.
///
/// @since 8.0
/// @author Steve Ebersole
public sealed interface UpdateRenderingPlan {
	/// Render the update directly using native syntax.
	record Direct() implements UpdateRenderingPlan {
	}

	/// Emulate the from-clause using scalar subqueries in the set clause.
	///
	/// A target alias selects correlated per-assignment subqueries.  An absent
	/// alias selects the standard internally constructed scalar-subquery form.
	record ScalarSubquery(@Nullable String targetAlias) implements UpdateRenderingPlan {
		/// Create the standard scalar-subquery plan without a target alias.
		public ScalarSubquery() {
			this( null );
		}
	}

	/// Emulate the update using a merge statement.
	record Merge() implements UpdateRenderingPlan {
	}

	/// Emulate the update using an inline view.
	record InlineView() implements UpdateRenderingPlan {
	}

	/// Emulate the update using tuple assignment.
	record TupleSet() implements UpdateRenderingPlan {
	}
}
