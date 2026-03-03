/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;

import org.hibernate.action.queue.plan.PlannedOperationGroup;

/**
 * @author Steve Ebersole
 */
public record GroupNode(PlannedOperationGroup group, long stableId) {
}
