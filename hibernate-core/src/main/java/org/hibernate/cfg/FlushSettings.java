/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

/// Settings related to flush behavior
///
/// @author Steve Ebersole
public interface FlushSettings {

	/// Specifies the [org.hibernate.action.queue.spi.ActionQueue] implementation to use.
	/// Valid values are defined by [org.hibernate.action.queue.spi.QueueType]:
	///
	///   - ["graph"][org.hibernate.action.queue.spi.QueueType#GRAPH] (default) - Uses the
	/// 	[graph-based][org.hibernate.action.queue.internal.GraphBasedActionQueue] ActionQueue implementation,
	/// 	which handles planning automatically based on defined constraints (foreign key dependencies,
	/// 	unique keys, ...).
	///   - ["legacy"][org.hibernate.action.queue.spi.QueueType#LEGACY] - Uses the
	/// 	[traditional][org.hibernate.engine.spi.ActionQueueLegacy] ActionQueue implementation,
	/// 	which requires hard-coded ordering and manual sorting of actions
	///
	/// @see org.hibernate.action.queue.spi.QueueType
	/// @see org.hibernate.action.queue.spi.ActionQueueFactory
	/// @see org.hibernate.action.queue.internal.support.ActionQueueFactoryService
	///
	/// @apiNote The legacy implementation remains available by explicitly setting
	/// this property to `"legacy"`.
	///
	/// @settingDefault `"graph"`
	///
	/// @since 8.0
	String FLUSH_QUEUE_TYPE = "hibernate.flush.queue.type";

	/// Whether to order operations by foreign-key as part of graph-based flush planning.
	///
	/// @see org.hibernate.action.queue.spi.PlanningOptions#orderByForeignKeys()
	///
	/// @settingDefault true
	///
	/// @since 8.0
	String ORDER_BY_FOREIGN_KEY = "hibernate.flush.plan.foreignkey.order";

	/// Whether to order operations by unique-key as part of graph-based flush planning.
	///
	/// @see org.hibernate.action.queue.spi.PlanningOptions#orderByUniqueKeySlots()
	///
	/// @settingDefault true
	///
	/// @since 8.0
	String ORDER_BY_UNIQUE_KEY = "hibernate.flush.plan.uniquekey.order";

	/// Controls whether deferrable constraints should create dependency
	/// [edges][org.hibernate.action.queue.internal.graph.GraphEdge]
	/// in the [flush graph][org.hibernate.action.queue.internal.graph.Graph].
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
	/// when a cycle is detected is still controlled by [#DEFERRABLE_AVOID_BREAK].
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

	/// Whether the graph-based action queue should defer IDENTITY inserts into the
	/// normal flush plan.
	///
	/// When {@code false} (default), the graph queue preserves legacy behavior and
	/// executes non-delayed IDENTITY inserts immediately so generated identifiers are
	/// visible after {@code persist()}.
	///
	/// When {@code true}, IDENTITY inserts are planned with the rest of the flush and
	/// generated identifiers are resolved through graph-generated value handles.
	///
	/// @settingDefault false
	///
	/// @since 8.0
	String GRAPH_DEFER_IDENTITY_INSERTS = "hibernate.flush.queue.graph.defer_identity_inserts";
}
