/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

/// Settings related to flush behavior
///
/// @author Steve Ebersole
public interface FlushSettings {

	/// Specifies the [org.hibernate.action.queue.ActionQueue] implementation to use.
	///
	/// Valid values:
	///
	///   - `"legacy"` - Uses the traditional ActionQueue implementation
	///     ([org.hibernate.engine.spi.ActionQueueLegacy]), which requires manual
	///     ordering of actions (default)
	///   - `"graph"` - Uses the graph-based ActionQueue implementation
	///     ([org.hibernate.action.queue.GraphBasedActionQueue]), which handles
	///     foreign key dependencies automatically through graph analysis
	///
	///
	/// The default is `"legacy"`. The graph-based implementation is experimental and
	/// provides improved handling of complex foreign key relationships and automatic
	/// dependency resolution.
	///
	/// @settingDefault `"legacy"`
	///
	/// @since 8.0
	String FLUSH_QUEUE_TYPE = "hibernate.flush.queue.impl";

	/// Whether to order operations by foreign-key as part of graph-based flush planning.
	///
	/// @see org.hibernate.action.queue.PlanningOptions#orderByForeignKeys()
	///
	/// @settingDefault true
	///
	/// @since 8.0
	String ORDER_BY_FOREIGN_KEY = "hibernate.flush.plan.foreignkey.order";

	/// Whether to order operations by unique-key as part of graph-based flush planning.
	///
	/// @see org.hibernate.action.queue.PlanningOptions#orderByUniqueKeySlots()
	///
	/// @settingDefault false
	///
	/// @since 8.0
	String ORDER_BY_UNIQUE_KEY = "hibernate.flush.plan.uniquekey.order";

	/// Controls whether deferrable constraints should create dependency
	/// [edges][org.hibernate.action.queue.graph.GraphEdge]
	/// in the [flush graph][org.hibernate.action.queue.graph.Graph].
	///
	/// Assuming a constraint is deferrable -
	///
	///	When true (default)
	/// 	- Hibernate expects the database will handle constraint checking at commit
	/// 	- Hibernate doesn't need to worry about execution order for these constraints
	/// 	- Result: Fewer edges → simpler graph → potentially better execution order
	///
	/// When false
	/// 	- Add deferrable constraints as graph edges even though DB can defer them
	/// 	- Hibernate still controls execution order
	/// 	- More conservative (works even if DB doesn't actually defer)
	///
	/// Note this simply creates edges.  Whether we decide to break these edges
	/// when a cycle is detected is still controlled by [#DEFERRABLE_EDGE_BREAK].
	/// The information is sometimes useful even though we may not break these edges.
	///
	/// @settingDefault true
	///
	/// @since 8.0
	String DEFERRABLE_EDGES_IGNORE = "hibernate.flush.plan.deferrable.ignore_edges";

	/// Controls whether deferrable constraints can be broken (nullified + fixup UPDATE)
	/// to resolve cycles.
	///
	/// Assuming a constraints is deferrable -
	///
	/// When true (default)
	/// 	- Avoid breaking deferrable constraints if possible
	/// 	- Prefer breaking non-deferrable constraints in cycles
	/// 	- Preference achieved by assigning a higher cost to breaking these edges
	/// 	- Only break deferrable constraints if no other option exists.  If the database can
	/// 		 defer the constraint, why break it? Let the database handle it.
	///
	/// When false
	/// 	- Deferrable constraints can be broken just like any other nullable constraint
	/// 	- If cycle breaking is needed, deferrable constraints are fair game
	///
	/// @settingDefault true
	///
	/// @since 8.0
	String DEFERRABLE_AVOID_BREAK = "hibernate.flush.plan.deferrable.avoid_break";

	/// Controls whether collection operations should be bundled into single
	/// [org.hibernate.action.queue.op.PlannedOperation] (one INSERT for all collection
	/// elements instead of one INSERT per element).
	///
	/// When true (default):
	/// 	- Collection inserts, updates, and deletes are bundled together
	/// 	- Results in fewer [org.hibernate.action.queue.op.PlannedOperation] (one per collection
	/// 		 instead of one per element)
	/// 	- Better performance for collections with many elements
	///
	/// When false:
	/// 	- Each collection element operation is a separate JDBC operation
	/// 	- More JDBC operations but simpler execution model
	///
	/// @settingDefault true
	///
	/// @since 8.0
	String BUNDLE_COLLECTION_OPERATIONS = "hibernate.flush.collection.bundle";
}
