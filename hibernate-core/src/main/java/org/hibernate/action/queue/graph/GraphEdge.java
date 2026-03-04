/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;

import java.util.Set;

/// Represents a foreign key dependency edge between two operation groups in the dependency graph.
/// This edge tells us the order in which we can execute the operations -
///
/// - For INSERT operations, the `parent` operation must always happen before the `child` operation.
/// - For DELETE operations, the reverse is true - the `child` must happen first.
///
/// E.g., given
///
/// ```java
/// @Entity @Table(name="customers")
/// class Customer {
/// 	@Id Integer id;
/// }
///
/// @Entity @Table(name="orders")
/// class Order {
/// 	@Id Integer id;
/// 	@ManyToOne @JoinColumn(name="customer_fk")
/// 	Customer customer;
/// }
/// ```
/// We might see a `GraphEdge` like:
///
/// ```
/// GraphEdge(
/// 	parent = GraphNode(
/// 		"insert into customers ...", ...
/// 	),
/// 	child = GraphNode(
/// 		"insert into orders ...", ...
/// 	)
/// )
/// ```
///
/// The edge tells us that we must perform `insert into customers ...` before
/// we can execute `insert into orders ...`
///
/// @see GraphBuilder
///
/// @author Steve Ebersole
public class GraphEdge {
	// The table that is the target of the foreign-key
	final GroupNode parent;
	// The table that is the key (source) of the foreign-key
	final GroupNode child;

	// At the moment, simply an "alias" for `parent`.
	final GroupNode from;
	// At the moment, simply an "alias" for `child`.
	final GroupNode to;

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

	/// The node/operation on the table that is the target of the foreign-key.
	public GroupNode getParent() {
		return parent;
	}

	/// The node/operation on the table that is the key (source) of the foreign-key.
	public GroupNode getChild() {
		return child;
	}

	/// Ultimately, whether the backing foreign-key is [nullable][org.hibernate.action.queue.fk.ForeignKey#nullable()].
	/// If the foreign-key is nullable, it means we can use that to help break cycles - the classic pattern of -
	///
	/// - insert into table_1 with null fk value
	/// - insert into table_2
	/// - update table_1 set fk to table_2
	public boolean isBreakable() {
		return breakable;
	}

	public int getBreakCost() {
		return breakCost;
	}

	/// If we do decide to break a cycle on this edge, this indicates the
	/// columns to -
	///
	/// - insert as null in the initial insert
	/// - update to set in the update
	public Set<String> getChildColumnsToNull() {
		return childColumnsToNull;
	}

	/// Whether the underlying foreign-key is defined as deferrable in the database.
	/// This basically means that we do not need to worry about trying to break cycles;
	/// the database will simply allow the 2 operations and check consistency at the end
	/// of the transaction.
	///
	/// @implNote This is currently not used, but is definitely something we want to use.
	public boolean isDeferrable() {
		return deferrable;
	}

	public long getStableId() {
		return stableId;
	}

	/// Whether we need to break a cycle at this edge.
	/// This will later be used by the FlushPlanner.
	public boolean isBroken() {
		return broken;
	}

	/// Break a cycle at this edge.
	public void setBroken(boolean broken) {
		this.broken = broken;
	}

	/// Same as [#getParent()].
	public GroupNode getFrom() {
		return from;
	}

	/// Same as [#getChild()].
	public GroupNode getTo() {
		return to;
	}
}
