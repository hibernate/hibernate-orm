/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.sql.model.PreparableMutationOperation;

import java.util.List;
import java.util.Objects;

import static org.hibernate.action.queue.Helper.normalizeTableName;

/// Helps identify similar mutations more concretely that just type+table.  This
/// takes into consideration [#shapeHash] as well, which might be based on columns
/// included / not included, etc.
///
/// Useful as a key for batching.
///
/// @author Steve Ebersole
public record StatementShapeKey(String tableExpression, MutationKind kind, int shapeHash) {

	public StatementShapeKey withType(MutationKind t) {
		return new StatementShapeKey(tableExpression, t, Objects.hash(tableExpression, t, shapeHash));
	}

	public static StatementShapeKey forInsert(String tableName, List<PlannedOperation> plannedOperations) {
		final String normalizedTableName = normalizeTableName( tableName );
		final int h = hashMutationShape(normalizedTableName, MutationKind.INSERT, plannedOperations);
		return new StatementShapeKey( normalizedTableName, MutationKind.INSERT, h);
	}

	public static StatementShapeKey forUpdate(String tableName, List<PlannedOperation> plannedOperations) {
		final String normalizedTableName = normalizeTableName( tableName );
		final int h = hashMutationShape(normalizedTableName, MutationKind.UPDATE, plannedOperations);
		return new StatementShapeKey( normalizedTableName, MutationKind.UPDATE, h);
	}

	public static StatementShapeKey forDelete(String tableName, List<PlannedOperation> plannedOperations) {
		final String normalizedTableName = normalizeTableName( tableName );
		final int h = hashMutationShape(normalizedTableName, MutationKind.DELETE, plannedOperations);
		return new StatementShapeKey( normalizedTableName, MutationKind.DELETE, h);
	}

	private static int hashMutationShape(String normalizedTableName, MutationKind kind, List<PlannedOperation> plannedOperations) {
		// Always include table + kind (guards against collisions)
		int hash = 17;
		hash = 31 * hash + normalizedTableName.hashCode();
		hash = 31 * hash + kind.hashCode();

		// Apply each planned operation
		for ( PlannedOperation plannedOperation : plannedOperations ) {
			if ( plannedOperation.getOperation() instanceof PreparableMutationOperation pmo ) {
				// prefer the SQL string
				hash = 31 * hash + pmo.getSqlString().hashCode();
			}
			else {
				hash = 31 * hash + plannedOperation.getOperation().hashCode();
			}
		}

		return hash;
	}
}
