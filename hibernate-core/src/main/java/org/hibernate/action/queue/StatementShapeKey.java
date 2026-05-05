/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue;

import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.action.queue.support.OperationGroupKey;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.sql.model.PreparableMutationOperation;

/// Helps identify similar mutations more concretely that just type+table.  This
/// takes into consideration [#shapeHash] as well, which might be based on columns
/// included / not included, etc.
///
/// Useful as a key for batching.
///
/// @author Steve Ebersole
public record StatementShapeKey(String tableExpression, MutationKind kind, int shapeHash) implements BatchKey, OperationGroupKey {

	public static StatementShapeKey forInsert(String tableName, PlannedOperation plannedOperation) {
		final int h = hashMutationShape(tableName, MutationKind.INSERT, plannedOperation);
		return new StatementShapeKey( tableName, MutationKind.INSERT, h);
	}

	public static StatementShapeKey forUpdate(String tableName, PlannedOperation plannedOperation) {
		final int h = hashMutationShape(tableName, MutationKind.UPDATE, plannedOperation);
		return new StatementShapeKey( tableName, MutationKind.UPDATE, h);
	}

	public static StatementShapeKey forUpdateOrder(String tableName, PlannedOperation plannedOperation) {
		final int h = hashMutationShape(tableName, MutationKind.UPDATE_ORDER, plannedOperation);
		return new StatementShapeKey( tableName, MutationKind.UPDATE_ORDER, h);
	}

	public static StatementShapeKey forDelete(String tableName, PlannedOperation plannedOperation) {
		final int h = hashMutationShape(tableName, MutationKind.DELETE, plannedOperation);
		return new StatementShapeKey( tableName, MutationKind.DELETE, h);
	}

	public static StatementShapeKey forNoOp(String tableName) {
		// No-op operations don't have jdbcOperation, just use table + kind for hash
		int hash = 17;
		hash = 31 * hash + tableName.hashCode();
		hash = 31 * hash + MutationKind.NO_OP.hashCode();
		return new StatementShapeKey( tableName, MutationKind.NO_OP, hash);
	}

	private static int hashMutationShape(
			String normalizedTableName,
			MutationKind kind,
			PlannedOperation plannedOperation) {
		// Always include table + kind (guards against collisions)
		int hash = 17;
		hash = 31 * hash + normalizedTableName.hashCode();
		hash = 31 * hash + kind.hashCode();

		// Include navigableRole for collections to distinguish multiple collections on same table
		var tableDescriptor = plannedOperation.getMutatingTableDescriptor();
		if ( tableDescriptor instanceof org.hibernate.action.queue.meta.CollectionTableDescriptor ctd ) {
			if ( ctd.navigableRole() != null ) {
				hash = 31 * hash + ctd.navigableRole().hashCode();
			}
		}

		if ( plannedOperation.getJdbcOperation() instanceof PreparableMutationOperation pmo ) {
			hash = 31 * hash + pmo.getSqlString().hashCode();
		}
		else {
			// we have some form of "self executing" operation
			hash = 31 * hash + plannedOperation.getJdbcOperation().hashCode();
		}

		return hash;
	}
}
