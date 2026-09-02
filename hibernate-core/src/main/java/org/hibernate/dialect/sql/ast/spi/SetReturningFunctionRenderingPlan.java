/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.Objects;

/// Immutable semantic plan for rendering a named set-returning function.
///
/// The plan selects invocation and ordinality syntax. The translator continues
/// to own argument traversal, aliases, and SQL emission.
///
/// @since 8.0
/// @author Steve Ebersole
public sealed interface SetReturningFunctionRenderingPlan {
	/// Render the native function invocation, optionally followed by native
	/// `with ordinality` syntax.
	record Native(Ordinality ordinality) implements SetReturningFunctionRenderingPlan {
		public Native {
			Objects.requireNonNull( ordinality, "ordinality" );
		}

		/// Native ordinality syntax to append to the invocation.
		public enum Ordinality {
			/// Do not render native ordinality syntax.
			NONE,
			/// Render `with ordinality`.
			WITH_ORDINALITY
		}
	}

	/// Wrap the invocation with the database's `table(...)` syntax.
	record TableWrapped() implements SetReturningFunctionRenderingPlan {
	}

	/// Emulate the requested ordinality column using a derived table.
	///
	/// @param invocationWrapper optional wrapper around the function invocation
	/// @param expression expression used to derive the ordinality value
	/// @param lateral whether the emulation must be rendered laterally
	record DerivedOrdinality(
			InvocationWrapper invocationWrapper,
			OrdinalityExpression expression,
			boolean lateral) implements SetReturningFunctionRenderingPlan {
		public DerivedOrdinality {
			Objects.requireNonNull( invocationWrapper, "invocationWrapper" );
			Objects.requireNonNull( expression, "expression" );
		}

		/// Wrapper applied to the function invocation inside the derived table.
		public enum InvocationWrapper {
			/// Render the invocation directly.
			NONE,
			/// Wrap the invocation in `table(...)`.
			TABLE
		}

		/// SQL expression used to synthesize ordinality.
		public enum OrdinalityExpression {
			/// `row_number()` using the database's ordinary empty window form.
			ROW_NUMBER,
			/// `row_number()` with a dummy ordering expression.
			ROW_NUMBER_DUMMY_ORDER,
			/// The database's `rownum` pseudo-column.
			ROWNUM
		}
	}

	/// Report that the requested invocation cannot be rendered safely. The
	/// translator raises an unsupported-operation failure.
	record Unsupported() implements SetReturningFunctionRenderingPlan {
	}
}
