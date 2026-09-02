/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;


/// Read-only semantic facts supplied by Hibernate when selecting how to render
/// a query update.
///
/// Implementations of [QueryMutationRenderingSupport] must treat this request
/// as call-scoped input and must not retain it.
///
/// @since 8.0
/// @author Steve Ebersole
public interface UpdateRenderingRequest {
	/// Whether the update contains a nontrivial from-clause.
	boolean hasNonTrivialFromClause();

	/// Whether the update requests generated values through returning columns.
	boolean hasReturningColumns();

	/// The native mutation capabilities reported by the Dialect. A rendering
	/// strategy may choose native syntax only when this profile supports the
	/// required capability for [MutationKind#UPDATE].
	MutationSyntaxSupport mutationSyntaxSupport();
}
