/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;

import org.hibernate.action.queue.plan.PlannedOperationGroup;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface GraphBuilder {
	Graph build(List<PlannedOperationGroup> groups);
}
