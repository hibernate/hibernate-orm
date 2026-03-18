/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;

import org.hibernate.action.queue.constraint.ForeignKey;
import org.hibernate.metamodel.mapping.SelectableMappings;

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
	final GroupNode targetNode;
	// The table that is the key (source) of the foreign-key
	final GroupNode keyNode;

	// incoming node (graph planning)
	final GroupNode from;
	// outgoing node (graph planning)
	final GroupNode to;

	final boolean breakable;
	final int breakCost;
	final SelectableMappings childColumnsToNull;

	final ForeignKey foreignKey;
	final long stableId;

	boolean broken;

	GraphEdge(
			GroupNode targetTableOpGroup,
			GroupNode keyTableOpGroup,
			GroupNode from,
			GroupNode to,
			boolean breakable,
			int breakCost,
			SelectableMappings childColumnsToNull,
			ForeignKey foreignKey,
		long stableId) {
		this.targetNode = targetTableOpGroup;
		this.keyNode = keyTableOpGroup;
		this.from = from;
		this.to = to;
		this.breakable = breakable;
		this.breakCost = breakCost;
		this.childColumnsToNull = childColumnsToNull;
		this.foreignKey = foreignKey;
		this.stableId = stableId;
	}

	///  The foreign key that this edge describes
	public ForeignKey getForeignKey() {
		return foreignKey;
	}

	/// The node/operation on the table that is the target of the foreign-key.
	public GroupNode getTargetNode() {
		return targetNode;
	}

	/// The node/operation on the table that is the key (source) of the foreign-key.
	public GroupNode getKeyNode() {
		return keyNode;
	}

	/// The incoming node (vertx), used in graph planning.
	public GroupNode getFrom() {
		return from;
	}

	/// The outgoing node (vertx), used in graph planning.
	public GroupNode getTo() {
		return to;
	}

	/// Ultimately, whether the backing foreign-key is [nullable][ForeignKey#nullable()].
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
	public SelectableMappings getChildColumnsToNull() {
		return childColumnsToNull;
	}

	/// Whether the underlying foreign-key is defined as deferrable in the database.
	/// This basically means that we do not need to worry about trying to break cycles;
	/// the database will simply allow the 2 operations and check consistency at the end
	/// of the transaction.
	///
	/// @implNote This is currently not used, but is definitely something we want to use.
	public boolean isDeferrable() {
		return foreignKey.deferrable();
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
}
