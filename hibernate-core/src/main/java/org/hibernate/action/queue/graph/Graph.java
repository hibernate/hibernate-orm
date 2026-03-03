/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;

import java.util.List;
import java.util.Map;

/**
 * @author Steve Ebersole
 */
public record Graph(List<GroupNode> nodes, Map<GroupNode, List<GraphEdge>> outgoing) {
}
