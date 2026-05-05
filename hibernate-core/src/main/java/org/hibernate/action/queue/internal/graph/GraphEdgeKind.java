/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.graph;


/// Describes the planner semantics of a graph edge.
///
/// @author Steve Ebersole
public enum GraphEdgeKind {
	/// A true ordering dependency that cycle breaking must preserve.
	REQUIRED_ORDER,

	/// A useful ordering preference that should be kept in acyclic plans but
	/// dropped before patchable dependencies when it participates in a cycle.
	PREFERRED_ORDER,

	/// A foreign-key dependency that can be broken by nulling the key columns
	/// and installing a later fixup.
	NULL_PATCHABLE_FK,

	/// A unique-constraint dependency that can be broken by nulling the unique
	/// columns and installing a later fixup.
	NULL_PATCHABLE_UNIQUE
}
