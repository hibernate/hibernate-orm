/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.plan;

import org.hibernate.action.queue.graph.Graph;


/**
 * @author Steve Ebersole
 */
public interface FlushPlanner {
	FlushPlan plan(Graph graph);
}
