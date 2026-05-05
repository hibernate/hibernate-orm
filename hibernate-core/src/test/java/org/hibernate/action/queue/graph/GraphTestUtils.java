/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.graph;


import org.hibernate.action.queue.internal.constraint.Constraint;
import org.hibernate.action.queue.internal.constraint.Deferrability;
import org.hibernate.action.queue.internal.constraint.ForeignKey;
import org.hibernate.action.queue.internal.constraint.UniqueConstraint;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectableMappings;

import static org.hibernate.action.queue.internal.graph.Util.EMPTY_SELECTABLES;

/// Test utilities for creating graph structures
///
/// @author Steve Ebersole
public class GraphTestUtils {
	private static SelectableMappings selectable(String tableName, String columnName) {
		final SelectableMapping selectableMapping = new SelectableMapping() {
			@Override
			public String getContainingTableExpression() {
				return tableName;
			}

			@Override
			public String getSelectionExpression() {
				return columnName;
			}

			@Override
			public String getCustomReadExpression() {
				return null;
			}

			@Override
			public String getCustomWriteExpression() {
				return null;
			}

			@Override
			public boolean isFormula() {
				return false;
			}

			@Override
			public boolean isNullable() {
				return true;
			}

			@Override
			public boolean isInsertable() {
				return true;
			}

			@Override
			public boolean isUpdateable() {
				return true;
			}

			@Override
			public boolean isPartitioned() {
				return false;
			}

			@Override
			public Long getLength() {
				return null;
			}

			@Override
			public Integer getArrayLength() {
				return null;
			}

			@Override
			public Integer getPrecision() {
				return null;
			}

			@Override
			public Integer getScale() {
				return null;
			}

			@Override
			public Integer getTemporalPrecision() {
				return null;
			}

			@Override
			public JdbcMapping getJdbcMapping() {
				return null;
			}
		};
		return new SelectableMappings() {
			@Override
			public int getJdbcTypeCount() {
				return 1;
			}

			@Override
			public SelectableMapping getSelectable(int columnIndex) {
				if ( columnIndex != 0 ) {
					throw new IndexOutOfBoundsException( columnIndex );
				}
				return selectableMapping;
			}

			@Override
			public int forEachSelectable(int offset, SelectableConsumer consumer) {
				consumer.accept( offset, selectableMapping );
				return 1;
			}
		};
	}

	/// Create a GraphEdge for testing purposes.
	public static GraphEdge createEdge(
			GroupNode targetNode,
			GroupNode keyNode,
			GroupNode from,
			GroupNode to,
			boolean breakable,
			int breakCost,
			SelectableMappings columnsToNull,
			Constraint constraint,
			long stableId) {
		if ( !breakable ) {
			return GraphEdge.requiredOrder( targetNode, keyNode, from, to, columnsToNull, constraint, stableId );
		}
		if ( constraint instanceof ForeignKey foreignKey ) {
			return GraphEdge.nullPatchableFk(
					targetNode,
					keyNode,
					from,
					to,
					columnsToNull,
					foreignKey,
					breakCost,
					stableId
			);
		}
		if ( constraint instanceof UniqueConstraint uniqueConstraint ) {
			return GraphEdge.nullPatchableUnique(
					targetNode,
					keyNode,
					from,
					to,
					columnsToNull,
					uniqueConstraint,
					breakCost,
					stableId
			);
		}
		return GraphEdge.preferredOrder( targetNode, keyNode, from, to, columnsToNull, constraint, breakCost, stableId );
	}

	/// Create a simple breakable edge with default parameters
	public static GraphEdge createBreakableEdge(GroupNode from, GroupNode to, int breakCost) {
		final ForeignKey fk = foreignKey(
				"key_table",
				"target_table",
				EMPTY_SELECTABLES,
				EMPTY_SELECTABLES,
				true
		);
		return createEdge(from, to, from, to, true, breakCost, EMPTY_SELECTABLES, fk, System.nanoTime());
	}

	/// Create an unbreakable edge
	public static GraphEdge createUnbreakableEdge(GroupNode from, GroupNode to) {
		final ForeignKey fk = foreignKey(
				"key_table",
				"target_table",
				EMPTY_SELECTABLES,
				EMPTY_SELECTABLES,
				false
		);
		return createEdge(from, to, from, to, false, Integer.MAX_VALUE, EMPTY_SELECTABLES, fk, System.nanoTime());
	}

	public static GraphEdge createPreferredOrderEdge(GroupNode from, GroupNode to) {
		return GraphEdge.preferredOrder(
				from,
				to,
				from,
				to,
				EMPTY_SELECTABLES,
				null,
				10,
				System.nanoTime()
		);
	}

	public static GraphEdge createNullPatchableFkEdge(GroupNode from, GroupNode to, int breakCost) {
		return createNullPatchableFkEdge( from, to, breakCost, System.nanoTime() );
	}

	public static GraphEdge createNullPatchableFkEdge(GroupNode from, GroupNode to, int breakCost, long stableId) {
		final SelectableMappings columns = selectable( from.group().tableExpression(), "fk_col" );
		final ForeignKey fk = foreignKey(
				from.group().tableExpression(),
				to.group().tableExpression(),
				columns,
				selectable( to.group().tableExpression(), "id" ),
				true
		);
		return GraphEdge.nullPatchableFk( to, from, from, to, columns, fk, breakCost, stableId );
	}

	public static GraphEdge createNullPatchableUniqueEdge(GroupNode from, GroupNode to, int breakCost) {
		return createNullPatchableUniqueEdge( from, to, breakCost, System.nanoTime() );
	}

	public static GraphEdge createNullPatchableUniqueEdge(GroupNode from, GroupNode to, int breakCost, long stableId) {
		final SelectableMappings columns = selectable( from.group().tableExpression(), "unique_col" );
		final UniqueConstraint uniqueConstraint = uniqueConstraint( from, columns );
		return GraphEdge.nullPatchableUnique( to, from, from, to, columns, uniqueConstraint, breakCost, stableId );
	}

	public static GraphEdge createLegacyRequiredUniqueEdge(GroupNode from, GroupNode to) {
		return createLegacyRequiredUniqueEdge( from, to, System.nanoTime() );
	}

	public static GraphEdge createLegacyRequiredUniqueEdge(GroupNode from, GroupNode to, long stableId) {
		final SelectableMappings columns = selectable( from.group().tableExpression(), "unique_col" );
		final UniqueConstraint uniqueConstraint = uniqueConstraint( from, columns );
		return GraphEdge.requiredOrder( to, from, from, to, columns, uniqueConstraint, stableId );
	}

	public static GraphEdge createLegacyRequiredFkEdge(GroupNode from, GroupNode to) {
		final SelectableMappings columns = selectable( from.group().tableExpression(), "fk_col" );
		final ForeignKey fk = foreignKey(
				from.group().tableExpression(),
				to.group().tableExpression(),
				columns,
				selectable( to.group().tableExpression(), "id" ),
				false
		);
		return GraphEdge.requiredOrder( to, from, from, to, columns, fk, System.nanoTime() );
	}

	private static ForeignKey foreignKey(
			String keyTable,
			String targetTable,
			SelectableMappings keyColumns,
			SelectableMappings targetColumns,
			boolean nullable) {
		return new ForeignKey(
				keyTable,
				targetTable,
				keyColumns,
				targetColumns,
				ForeignKey.TargetType.NON_UNIQUE,
				true,
				nullable,
				Deferrability.NOT_DEFERRABLE
		);
	}

	private static UniqueConstraint uniqueConstraint(GroupNode node, SelectableMappings columns) {
		return new UniqueConstraint(
				node.group().tableExpression(),
				"uk_test",
				UniqueConstraint.ConstraintType.UNIQUE_KEY,
				columns,
				Deferrability.NOT_DEFERRABLE,
				true,
				new String[] { "uniqueValue" }
		);
	}
}
