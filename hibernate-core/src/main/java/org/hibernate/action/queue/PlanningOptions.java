/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

/// Options for controlling operation planning and scheduling.
///
/// @author Steve Ebersole
public record PlanningOptions(
	boolean orderByForeignKeys,
	boolean orderByUniqueKeySlots,
	boolean avoidBreakingDeferrable,
	boolean ignoreDeferrableForOrdering,
	UniqueCycleStrategy uniqueCycleStrategy) {

	public enum UniqueCycleStrategy {
		FAIL,
		IGNORE_UNIQUE_EDGES_IN_CYCLES
	}
}
