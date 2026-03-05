/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.sql.model.PreparableMutationOperation;

import java.util.Objects;

import static org.hibernate.action.queue.Helper.normalizeTableName;

/// Helps identify similar mutations more concretely that just type+table.  This
/// takes into consideration [#shapeHash] as well, which might be based on columns
/// included / not included, etc.
///
/// Useful as a key for batching.
///
/// @author Steve Ebersole
public record StatementShapeKey(String tableExpression, MutationKind kind, int shapeHash) implements BatchKey {

	public StatementShapeKey withType(MutationKind t) {
		return new StatementShapeKey(tableExpression, t, Objects.hash(tableExpression, t, shapeHash));
	}

	public static StatementShapeKey forInsert(String tableName, PlannedOperation plannedOperation) {
		final String normalizedTableName = normalizeTableName( tableName );
		final int h = hashMutationShape(normalizedTableName, MutationKind.INSERT, plannedOperation);
		return new StatementShapeKey( normalizedTableName, MutationKind.INSERT, h);
	}

	public static StatementShapeKey forUpdate(String tableName, PlannedOperation plannedOperation) {
		final String normalizedTableName = normalizeTableName( tableName );
		final int h = hashMutationShape(normalizedTableName, MutationKind.UPDATE, plannedOperation);
		return new StatementShapeKey( normalizedTableName, MutationKind.UPDATE, h);
	}

	public static StatementShapeKey forDelete(String tableName, PlannedOperation plannedOperation) {
		final String normalizedTableName = normalizeTableName( tableName );
		final int h = hashMutationShape(normalizedTableName, MutationKind.DELETE, plannedOperation);
		return new StatementShapeKey( normalizedTableName, MutationKind.DELETE, h);
	}

	private static int hashMutationShape(
			String normalizedTableName,
			MutationKind kind,
			PlannedOperation plannedOperation) {
		// Always include table + kind (guards against collisions)
		int hash = 17;
		hash = 31 * hash + normalizedTableName.hashCode();
		hash = 31 * hash + kind.hashCode();

		if ( plannedOperation.getOperation() instanceof PreparableMutationOperation pmo ) {
			hash = 31 * hash + pmo.getSqlString().hashCode();
		}
		else {
			// we have some form of "self executing" operation
			hash = 31 * hash + plannedOperation.getOperation().hashCode();
		}

		return hash;
	}
}
