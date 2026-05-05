/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.graph;


import org.hibernate.action.queue.internal.constraint.Constraint;
import org.hibernate.action.queue.internal.constraint.DeferrableConstraintMode;
import org.hibernate.action.queue.internal.constraint.ForeignKey;
import org.hibernate.action.queue.internal.constraint.UniqueConstraint;
import org.hibernate.action.queue.internal.cyclebreak.BindingPatch;
import org.hibernate.metamodel.mapping.SelectableMappings;

/// Represents an ordering edge between two operation groups in the dependency graph.
/// An edge may describe a foreign-key dependency, a unique-slot dependency, or a
/// planner ordering preference. The `from` operation should execute before the
/// `to` operation unless the planner explicitly breaks the edge.
///
/// For foreign-key edges:
///
/// - INSERT edges usually order the referenced row before the row containing the key.
/// - DELETE edges usually reverse that order so key rows are removed first.
///
/// Unique-slot edges order operations that release a unique value before operations
/// that occupy the same value. Nullable FK and nullable unique edges can also carry
/// the columns needed for a temporary NULL plus later fixup.
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
/// See [GraphBuilder].
///
/// @author Steve Ebersole
public class GraphEdge {
	// The referenced/target side for FK edges; may be null for unique-slot edges.
	final GroupNode targetNode;
	// The key side for FK edges, or the operation that receives a patch for unique-slot edges.
	final GroupNode keyNode;

	// incoming node (graph planning)
	final GroupNode from;
	// outgoing node (graph planning)
	final GroupNode to;

	final int breakCost;
	final SelectableMappings columnsToNull;

	final Constraint constraint;
	final GraphEdgeKind kind;
	final long stableId;

	boolean broken;

	static GraphEdge requiredOrder(
			GroupNode targetTableOpGroup,
			GroupNode keyTableOpGroup,
			GroupNode from,
			GroupNode to,
			SelectableMappings columnsToNull,
			Constraint constraint,
			long stableId) {
		return new GraphEdge(
				targetTableOpGroup,
				keyTableOpGroup,
				from,
				to,
				0,
				columnsToNull,
				constraint,
				GraphEdgeKind.REQUIRED_ORDER,
				stableId
		);
	}

	static GraphEdge preferredOrder(
			GroupNode targetTableOpGroup,
			GroupNode keyTableOpGroup,
			GroupNode from,
			GroupNode to,
			SelectableMappings columnsToNull,
			Constraint constraint,
			int breakCost,
			long stableId) {
		return new GraphEdge(
				targetTableOpGroup,
				keyTableOpGroup,
				from,
				to,
				breakCost,
				columnsToNull,
				constraint,
				GraphEdgeKind.PREFERRED_ORDER,
				stableId
		);
	}

	static GraphEdge nullPatchableFk(
			GroupNode targetTableOpGroup,
			GroupNode keyTableOpGroup,
			GroupNode from,
			GroupNode to,
			SelectableMappings columnsToNull,
			ForeignKey foreignKey,
			int breakCost,
			long stableId) {
		return new GraphEdge(
				targetTableOpGroup,
				keyTableOpGroup,
				from,
				to,
				breakCost,
				columnsToNull,
				foreignKey,
				GraphEdgeKind.NULL_PATCHABLE_FK,
				stableId
		);
	}

	static GraphEdge nullPatchableUnique(
			GroupNode targetTableOpGroup,
			GroupNode keyTableOpGroup,
			GroupNode from,
			GroupNode to,
			SelectableMappings columnsToNull,
			UniqueConstraint uniqueConstraint,
			int breakCost,
			long stableId) {
		return new GraphEdge(
				targetTableOpGroup,
				keyTableOpGroup,
				from,
				to,
				breakCost,
				columnsToNull,
				uniqueConstraint,
				GraphEdgeKind.NULL_PATCHABLE_UNIQUE,
				stableId
		);
	}

	private GraphEdge(
			GroupNode targetTableOpGroup,
			GroupNode keyTableOpGroup,
			GroupNode from,
			GroupNode to,
			int breakCost,
			SelectableMappings columnsToNull,
			Constraint constraint,
			GraphEdgeKind kind,
			long stableId) {
		this.targetNode = targetTableOpGroup;
		this.keyNode = keyTableOpGroup;
		this.from = from;
		this.to = to;
		this.breakCost = breakCost;
		this.columnsToNull = columnsToNull;
		this.constraint = constraint;
		this.kind = kind;
		this.stableId = stableId;
	}

	/// The constraint that this edge describes (might be a foreign key or unique constraint)
	public Constraint getConstraint() {
		return constraint;
	}

	public GraphEdgeKind getKind() {
		return kind;
	}

	public boolean isPreferredOrder() {
		return kind == GraphEdgeKind.PREFERRED_ORDER;
	}

	public boolean isRequiredOrder() {
		return kind == GraphEdgeKind.REQUIRED_ORDER;
	}

	public boolean isNullPatchableForeignKey() {
		return kind == GraphEdgeKind.NULL_PATCHABLE_FK;
	}

	public boolean isNullPatchableUnique() {
		return kind == GraphEdgeKind.NULL_PATCHABLE_UNIQUE;
	}

	public boolean isNullPatchable() {
		return isNullPatchableForeignKey() || isNullPatchableUnique();
	}

	/// The foreign key that this edge describes, or null if this is a unique constraint edge
	public ForeignKey getForeignKey() {
		return constraint instanceof ForeignKey fk ? fk : null;
	}

	/// Whether this edge represents a foreign key dependency (as opposed to a unique constraint dependency)
	public boolean isForeignKeyEdge() {
		return constraint instanceof ForeignKey;
	}

	public boolean isUniqueConstraintEdge() {
		return constraint instanceof UniqueConstraint;
	}

	public boolean isUniqueCycleEdge() {
		return switch ( kind ) {
			case NULL_PATCHABLE_UNIQUE -> true;
			case REQUIRED_ORDER -> isUniqueConstraintEdge();
			case NULL_PATCHABLE_FK, PREFERRED_ORDER -> false;
		};
	}

	public BindingPatch.CycleType getPatchCycleType() {
		return switch ( kind ) {
			case NULL_PATCHABLE_FK -> BindingPatch.CycleType.FOREIGN_KEY;
			case NULL_PATCHABLE_UNIQUE -> BindingPatch.CycleType.UNIQUE_SWAP;
			case REQUIRED_ORDER, PREFERRED_ORDER -> null;
		};
	}

	/// The referenced/target-side operation for FK edges, or null when the edge
	/// is not tied to a referenced table.
	public GroupNode getTargetNode() {
		return targetNode;
	}

	/// The key-side operation for FK edges, or the operation that receives the
	/// NULL-then-fixup patch for unique-slot edges.
	public GroupNode getKeyNode() {
		return keyNode;
	}

	/// The operation group that should receive a cycle-breaking patch.
	public GroupNode getPatchNode() {
		return keyNode;
	}

	/// The source node, used in graph planning.
	public GroupNode getFrom() {
		return from;
	}

	/// The target node, used in graph planning.
	public GroupNode getTo() {
		return to;
	}

	/// Whether this edge can be broken by temporarily nulling columns and
	/// installing a later fixup.
	///
	/// For nullable FK cycles this is the classic insert-null, insert-other-row,
	/// update-FK pattern. For nullable unique swaps this temporarily clears the
	/// unique value on one UPDATE and restores it afterward.
	public boolean isBreakable() {
		return isNullPatchable();
	}

	public int getBreakCost() {
		return breakCost;
	}

	/// Columns to temporarily null when breaking this edge.
	public SelectableMappings getColumnsToNull() {
		return columnsToNull;
	}

	/// Whether the underlying constraint is defined as deferrable in the database.
	/// This basically means that we do not need to worry about trying to break cycles;
	/// the database will simply allow the 2 operations and check consistency at the end
	/// of the transaction.
	///
	/// @implNote This is currently not used, but is definitely something we want to use.
	public boolean isDeferrable() {
		return constraint != null && constraint.isDeferrable();
	}

	public boolean isEffectivelyDeferred(DeferrableConstraintMode deferrableConstraintMode) {
		return constraint != null && deferrableConstraintMode.isDeferred( constraint.getDeferrability() );
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
