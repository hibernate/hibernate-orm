/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

/// Narrow contracts for coordinator-driven component binding phases.
///
/// Components are value mappings, not managed-type binders, so their phase
/// participants are collected while component values are materialized and then
/// drained by the model-binding coordinator.
///
/// @since 9.0
/// @author Steve Ebersole
public interface ComponentBindingPhase {
	/// Apply custom component mapping after the structural component mapping
	/// object is available and before member attribute/value custom mapping.
	interface CustomMapping {
		void bindCustomMapping();
	}

	/// Finalize aggregate component mapping after member values, table keys, and
	/// foreign-key binding have populated the component/table model.
	interface AggregateFinalization {
		void finishAggregateMapping();
	}
}
