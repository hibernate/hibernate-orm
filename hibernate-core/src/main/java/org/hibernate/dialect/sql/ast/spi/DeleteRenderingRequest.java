/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;


/// Read-only semantic facts supplied by Hibernate when selecting how to render
/// a query delete.
///
/// Implementations of [QueryMutationRenderingSupport] must treat this request
/// as call-scoped input and must not retain it.
///
/// The standard policy selects [DeleteRenderingPlan.Direct] when
/// [#hasNonTrivialFromClause] is false or native delete joins are supported. It
/// otherwise selects [DeleteRenderingPlan.JoinEmulation]. A family policy may
/// additionally consider [#hasReturningColumns] when a returning wrapper is
/// incompatible with a particular native or emulated delete form.
///
/// @since 8.0
/// @author Steve Ebersole
public interface DeleteRenderingRequest {
	/// Whether the semantic delete requires more than its target table alone.
	///
	/// This is `true` when its from clause has multiple roots or when its first
	/// root contains a real table-group or table-reference join. It is `false`
	/// for the ordinary target-only form, even though that target is represented
	/// by a from-clause root in the SQL AST.
	boolean hasNonTrivialFromClause();

	/// Whether one or more columns must be returned from the deleted rows.
	///
	/// This fact lets a family avoid a delete plan which cannot be nested in the
	/// form selected by [ReturningRenderingSupport]. It does not itself select or
	/// render the returning syntax.
	boolean hasReturningColumns();

	/// The Dialect's immutable native mutation-syntax profile.
	///
	/// A policy should query this profile with [MutationKind#DELETE]. In
	/// particular, [MutationSyntaxCapability#JOIN] means that the translator's
	/// direct delete grammar may preserve a nontrivial from clause without join
	/// emulation. Capabilities describe native syntax, not behavior which the
	/// translator could emulate.
	MutationSyntaxSupport mutationSyntaxSupport();
}
