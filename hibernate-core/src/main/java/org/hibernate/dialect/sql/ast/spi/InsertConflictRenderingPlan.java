/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.Objects;

/// Immutable plan for rendering insert-conflict semantics.
///
/// Providers select the syntax family. The translator continues to own target,
/// predicate, assignment, and value traversal.
///
/// @since 8.0
/// @author Steve Ebersole
public sealed interface InsertConflictRenderingPlan {
	/// Render an ordinary insert because no conflict action was requested.
	///
	/// For example, `insert into book (id, title) values (?, ?)` is rendered with
	/// no additional clause. Selecting this plan for either
	/// [InsertConflictAction#DO_NOTHING] or [InsertConflictAction#DO_UPDATE] is an
	/// error because it would silently discard requested semantics.
	record None() implements InsertConflictRenderingPlan {
	}

	/// Render an ordinary single-row insert and emulate `do nothing` by handling
	/// an eligible unique-constraint violation during execution.
	///
	/// No conflict SQL is appended. Instead, the translated JDBC operation
	/// records the constraint which may fail so execution can suppress the
	/// matching violation. This plan is valid only for
	/// [InsertConflictAction#DO_NOTHING], and only when the request has no
	/// constraint-column list or predicate. The emulation is limited to an insert
	/// of at most one row.
	record ConstraintViolation() implements InsertConflictRenderingPlan {
	}

	/// Render standard `on conflict` syntax from the complete semantic conflict
	/// clause.
	///
	/// Examples include:
	///
	/// ```sql
	/// insert into book (isbn, title) values (?, ?)
	/// on conflict (isbn) do nothing
	/// ```
	///
	/// and:
	///
	/// ```sql
	/// insert into book (isbn, title) values (?, ?)
	/// on conflict (isbn) do update set title = excluded.title
	/// ```
	///
	/// Constraint names, constraint-column lists, update assignments, and an
	/// update predicate remain owned by the translator.
	record Standard() implements InsertConflictRenderingPlan {
	}

	/// Render an `on duplicate key update` clause.
	///
	/// For [InsertConflictAction#DO_UPDATE], the usual shape is:
	///
	/// ```sql
	/// insert into book (isbn, title) values (?, ?)
	/// on duplicate key update title = values(title)
	/// ```
	///
	/// [#valuesRowReferenceStyle] controls whether proposed values are expressed
	/// using `values(title)`, an explicit `excluded` row alias, or the database's
	/// implicit `excluded.title` pseudo-row.
	///
	/// Because this syntax has no native do-nothing form on some databases,
	/// [#doNothingSyntax] selects either a harmless self-assignment such as
	/// `isbn=isbn`, or a `nothing` keyword after `on duplicate key update`.
	/// Constraint names are not supported by this plan.
	record OnDuplicateKey(
			DoNothingSyntax doNothingSyntax,
			ValuesRowReferenceStyle valuesRowReferenceStyle) implements InsertConflictRenderingPlan {
		public OnDuplicateKey {
			Objects.requireNonNull( doNothingSyntax, "doNothingSyntax" );
			Objects.requireNonNull( valuesRowReferenceStyle, "valuesRowReferenceStyle" );
		}
	}

	/// Render a do-update insert as a `merge` statement.
	///
	/// The translator constructs a proposed-row source named `excluded`, joins it
	/// to the target using the conflict columns, renders conflict assignments in
	/// `when matched`, and renders the insert in `when not matched`:
	///
	/// ```sql
	/// merge into book b using (...) excluded
	/// on (b.isbn = excluded.isbn)
	/// when matched then update set title = excluded.title
	/// when not matched then insert (...) values (...)
	/// ```
	///
	/// This plan is valid only for [InsertConflictAction#DO_UPDATE] and does not
	/// support an explicitly named constraint.
	///
	/// @param terminateStatement whether the generated merge must end with the
	/// database's statement terminator
	record Merge(boolean terminateStatement) implements InsertConflictRenderingPlan {
	}
}
