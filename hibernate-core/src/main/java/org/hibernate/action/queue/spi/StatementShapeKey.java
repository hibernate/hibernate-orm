/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi;

import org.hibernate.Incubating;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.action.queue.internal.support.OperationGroupKey;
import org.hibernate.action.queue.spi.meta.CollectionTableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.PreparableMutationOperation;

/// Helps identify similar mutations more concretely that just type+table.  This
/// takes into consideration [#shapeHash] as well, which might be based on columns
/// included / not included, etc.
///
/// Useful as a key for batching.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public record StatementShapeKey(String tableExpression, MutationKind kind, int shapeHash) implements BatchKey, OperationGroupKey {

	public static StatementShapeKey forInsert(String tableName, FlushOperation flushOperation) {
		final int h = hashMutationShape(tableName, MutationKind.INSERT, flushOperation);
		return new StatementShapeKey( tableName, MutationKind.INSERT, h);
	}

	public static StatementShapeKey forUpdate(String tableName, FlushOperation flushOperation) {
		final int h = hashMutationShape(tableName, MutationKind.UPDATE, flushOperation);
		return new StatementShapeKey( tableName, MutationKind.UPDATE, h);
	}

	public static StatementShapeKey forUpdateOrder(String tableName, FlushOperation flushOperation) {
		final int h = hashMutationShape(tableName, MutationKind.UPDATE_ORDER, flushOperation);
		return new StatementShapeKey( tableName, MutationKind.UPDATE_ORDER, h);
	}

	public static StatementShapeKey forDelete(String tableName, FlushOperation flushOperation) {
		final int h = hashMutationShape(tableName, MutationKind.DELETE, flushOperation);
		return new StatementShapeKey( tableName, MutationKind.DELETE, h);
	}

	public static StatementShapeKey forNoOp(String tableName) {
		// No-op operations don't have jdbcOperation, just use table + kind for hash
		int hash = 17;
		hash = 31 * hash + tableName.hashCode();
		hash = 31 * hash + MutationKind.NO_OP.hashCode();
		return new StatementShapeKey( tableName, MutationKind.NO_OP, hash);
	}

	public static StatementShapeKey forMutation(
			String tableName,
			MutationKind kind,
			TableDescriptor tableDescriptor,
			MutationOperation mutationOperation) {
		final int h = hashMutationShape( tableName, kind, tableDescriptor, mutationOperation );
		return new StatementShapeKey( tableName, kind, h );
	}

	private static int hashMutationShape(
			String normalizedTableName,
			MutationKind kind,
			FlushOperation flushOperation) {
		return hashMutationShape(
				normalizedTableName,
				kind,
				flushOperation.getMutatingTableDescriptor(),
				flushOperation.getJdbcOperation()
		);
	}

	private static int hashMutationShape(
			String normalizedTableName,
			MutationKind kind,
			TableDescriptor tableDescriptor,
			MutationOperation mutationOperation) {
		// Always include table + kind (guards against collisions)
		int hash = 17;
		hash = 31 * hash + normalizedTableName.hashCode();
		hash = 31 * hash + kind.hashCode();

		// Include navigableRole for collections to distinguish multiple collections on same table
		if ( tableDescriptor instanceof CollectionTableDescriptor ctd ) {
			if ( ctd.navigableRole() != null ) {
				hash = 31 * hash + ctd.navigableRole().hashCode();
			}
		}

		if ( mutationOperation instanceof PreparableMutationOperation pmo ) {
			hash = 31 * hash + pmo.getSqlString().hashCode();
		}
		else {
			// we have some form of "self executing" operation
			hash = 31 * hash + mutationOperation.hashCode();
		}

		return hash;
	}
}
