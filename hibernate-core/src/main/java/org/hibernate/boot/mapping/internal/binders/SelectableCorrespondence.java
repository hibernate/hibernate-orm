/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import jakarta.annotation.Nonnull;
import org.hibernate.mapping.Column;

/// A paired local/referenced selectable correspondence used while creating
/// order-sensitive foreign-key constraints.
///
/// The important invariant is that ordering is applied to this pair, not to
/// independent local and referenced column lists.
///
/// @param localColumn The local foreign-key column owned by the association, collection key, derived-id value,
/// 		or table key being bound.
/// @param referencedColumn The target column that [#localColumn()] references.
/// @param localSourcePosition The zero-based position of [#localColumn()] in the local source/binding order before
/// 		correspondence entries are reordered to match the target side.  This is mainly diagnostic state and a bridge
/// 		for callers that still need to project back to local declaration order.
/// @param targetPosition The zero-based position of [#referencedColumn()] in the target identifier, referenced
/// 		property, or component leaf traversal order. Sorting correspondences by this value produces the order
/// 		expected by foreign-key constraint creation and runtime identifier/component traversal.
/// @param sourceRole Human-readable role used in diagnostics, such as the owning property path, collection role,
/// 		or table-key role that produced the correspondence.
///
/// @since 9.0
/// @author Steve Ebersole
public record SelectableCorrespondence(
		@Nonnull Column localColumn,
		@Nonnull Column referencedColumn,
		int localSourcePosition,
		int targetPosition,
		@Nonnull String sourceRole) {
}
