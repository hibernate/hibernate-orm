/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

/**
 * @author Steve Ebersole
 */
public record GraphPlanningConfiguration(
	boolean orderByForeignKeys,
	boolean orderByUniqueKeySlots,
	UniqueCycleStrategy uniqueCycleStrategy) {

	public enum UniqueCycleStrategy {
		FAIL,
		IGNORE_UNIQUE_EDGES_IN_CYCLES
	}
}
