/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.Objects;

/// Immutable plan selected by [ReturningRenderingSupport] for returning
/// generated mutation values.
///
/// The translator owns column traversal and SQL emission; providers only choose
/// the supported syntax family.
///
/// @since 8.0
/// @author Steve Ebersole
public sealed interface ReturningRenderingPlan {
	/// Render a standard trailing `returning` clause after the mutation.
	///
	/// Examples include `insert into book (...) values (...) returning id` and
	/// `update book set title = ? returning id, version`.
	///
	/// This plan requires at least one [ReturningRenderingRequest#returningColumns
	/// returning column].
	record Clause() implements ReturningRenderingPlan {
	}

	/// Wrap the mutation in an `old`, `new`, or `final` data-change table and
	/// select the requested columns from the wrapper.
	///
	/// For example, [ChangeTableKind#FINAL] produces the following general shape:
	///
	/// ```sql
	/// select id, version
	/// from final table (
	///     update book set title = ? where id = ?
	/// )
	/// ```
	///
	/// [ChangeTableKind#OLD] similarly exposes rows before a delete, while
	/// [ChangeTableKind#NEW] exposes the database's post-mutation, pre-trigger row
	/// image. This plan requires at least one returning column.
	record ChangeTable(ChangeTableKind kind) implements ReturningRenderingPlan {
		public ChangeTable {
			Objects.requireNonNull( kind, "kind" );
		}
	}

	/// Render only the mutation because no columns were requested.
	///
	/// This is not an "unsupported returning" result. Selecting it when
	/// [ReturningRenderingRequest#returningColumns] is nonempty causes translation
	/// to fail instead of silently dropping the returned values.
	record None() implements ReturningRenderingPlan {
	}
}
