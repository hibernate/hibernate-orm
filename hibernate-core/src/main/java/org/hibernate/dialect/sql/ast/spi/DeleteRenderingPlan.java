/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import jakarta.annotation.Nullable;

/// Immutable semantic plan for rendering a query delete.
///
/// Providers select the native or emulated form. The translator owns SQL AST
/// traversal, mutation predicates, and SQL emission.
///
/// A plan is selected once for a delete statement based on a
/// [DeleteRenderingRequest]. It controls how joins represented by the delete's
/// from clause are expressed; it does not replace
/// [ReturningRenderingSupport], which independently controls returned columns.
///
/// @since 8.0
/// @author Steve Ebersole
public sealed interface DeleteRenderingPlan {
	/// Render the delete directly using the translator's native delete grammar.
	///
	/// For a simple delete, the result has the usual shape:
	///
	/// ```sql
	/// delete from book where book.id = ?
	/// ```
	///
	/// A translator may also render database-specific joined-delete syntax from
	/// this plan, for example `delete b from book b join author a ...`. The plan
	/// is therefore appropriate when there is no nontrivial from clause, or when
	/// [MutationSyntaxSupport] reports native [MutationSyntaxCapability#JOIN]
	/// support for [MutationKind#DELETE].
	record Direct() implements DeleteRenderingPlan {
	}

	/// Move joins which cannot appear directly in the delete into a correlated
	/// `exists` subquery in the where clause.
	///
	/// A joined semantic delete is rendered with the following general shape:
	///
	/// ```sql
	/// delete from book
	/// where exists (
	///     select 1
	///     from author
	///     where author.id = book.author_id and ...
	/// )
	/// ```
	///
	/// When `targetAlias` is non-null, the target is declared using that alias and
	/// the subquery includes a row-matching correlation between the target alias
	/// and the target table reference in the emulated from clause. This form is
	/// used by databases which require an explicit target alias to correlate the
	/// emulation safely.
	///
	/// @param targetAlias alias used to correlate the emulation, or `null` for
	/// the standard unaliased form
	record JoinEmulation(@Nullable String targetAlias) implements DeleteRenderingPlan {
		/// Select the standard, unaliased join-emulation form.
		public JoinEmulation() {
			this( null );
		}
	}
}
