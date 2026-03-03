/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;

import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class GraphEdge {
	final GroupNode parent; // parent insert group (toTable)
	final GroupNode child;  // child insert group (fromTable)

	final GroupNode from;   // alias parent (edge direction for topo: parent -> child)
	final GroupNode to;     // alias child

	final boolean breakable;
	final int breakCost;
	final Set<String> childColumnsToNull;
	final boolean deferrable;
	final long stableId;

	boolean broken;

	GraphEdge(GroupNode parent,
		GroupNode child,
		boolean breakable,
		int breakCost,
		Set<String> childColumnsToNull,
		boolean deferrable,
		long stableId) {

		this.parent = parent;
		this.child = child;
		this.from = parent;
		this.to = child;
		this.breakable = breakable;
		this.breakCost = breakCost;
		this.childColumnsToNull = childColumnsToNull;
		this.deferrable = deferrable;
		this.stableId = stableId;
	}

	public GroupNode getParent() {
		return parent;
	}

	public GroupNode getChild() {
		return child;
	}

	public GroupNode getFrom() {
		return from;
	}

	public GroupNode getTo() {
		return to;
	}

	public boolean isBreakable() {
		return breakable;
	}

	public int getBreakCost() {
		return breakCost;
	}

	public Set<String> getChildColumnsToNull() {
		return childColumnsToNull;
	}

	public boolean isDeferrable() {
		return deferrable;
	}

	public long getStableId() {
		return stableId;
	}

	public boolean isBroken() {
		return broken;
	}

	public void setBroken(boolean broken) {
		this.broken = broken;
	}
}
