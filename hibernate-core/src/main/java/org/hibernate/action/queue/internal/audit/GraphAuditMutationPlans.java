/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.audit;

import java.sql.SQLException;

import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.bind.OperationResultChecker;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.audit.ModificationType;
import org.hibernate.audit.spi.AuditChangeSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.mutation.CollectionAuditSupport;
import org.hibernate.persister.collection.mutation.CollectionAuditSupport.AuditCollectionChange;
import org.hibernate.persister.entity.mutation.EntityAuditSupport;
import org.hibernate.persister.entity.mutation.EntityAuditSupport.AuditInsertPlan;
import org.hibernate.persister.entity.mutation.EntityAuditSupport.TransactionEndPlan;

/// Graph-audit helpers used while materializing merged audit changes as
/// ordinary [FlushOperation] instances.
///
/// The collector owns creation of [FlushOperation] instances. These helpers keep
/// the queue-local bind plans, result checking, and operation descriptions out of
/// the collector's change-set orchestration code.
final class GraphAuditMutationPlans {
	private GraphAuditMutationPlans() {
	}

	record EntityAuditInsertPlan(
			EntityAuditSupport mutationSupport,
			AuditInsertPlan plan) {
		BindPlan createBindPlan(
				AuditChangeSet.EntityChange<EntityAuditSupport> change,
				Object changesetId) {
			final var operation = plan.operation();
			return new EntityAuditInsertBindPlan(
					mutationSupport,
					operation.tableIndex(),
					change.entityKey().getIdentifier(),
					change.values(),
					plan.propertyInclusions(),
					change.modificationType(),
					changesetId
			);
		}

		String describe(AuditChangeSet.EntityChange<EntityAuditSupport> change) {
			return "GraphAuditEntity(" + change.entityKey().getEntityName() + "#insert)";
		}
	}

	record EntityTransactionEndPlan(
			EntityAuditSupport mutationSupport,
			TransactionEndPlan plan) {
		BindPlan createBindPlan(
				AuditChangeSet.EntityChange<EntityAuditSupport> change,
				Object changesetId) {
			final var operation = plan.operation();
			return new EntityTransactionEndBindPlan(
					mutationSupport,
					operation.tableIndex(),
					change.entityKey().getIdentifier(),
					change.modificationType(),
					changesetId
			);
		}

		String describe(AuditChangeSet.EntityChange<EntityAuditSupport> change) {
			return "GraphAuditEntity(" + change.entityKey().getEntityName() + "#end)";
		}
	}

	record CollectionAuditInsertPlan(
			CollectionAuditSupport mutationSupport,
			CollectionAuditSupport.AuditInsertPlan plan) {
		BindPlan createBindPlan(CollectionAuditRowChange rowChange, Object changesetId) {
			return new CollectionAuditInsertBindPlan(
					mutationSupport,
					rowChange.collection(),
					rowChange.ownerId(),
					rowChange.change(),
					changesetId
			);
		}

		String describe() {
			return "GraphAuditCollection(" + mutationSupport.getMutationTarget().getRolePath() + "#insert)";
		}
	}

	record CollectionTransactionEndPlan(
			CollectionAuditSupport mutationSupport,
			CollectionAuditSupport.TransactionEndPlan plan) {
		BindPlan createBindPlan(CollectionAuditRowChange rowChange, Object changesetId) {
			return new CollectionTransactionEndBindPlan(
					mutationSupport,
					rowChange.collection(),
					rowChange.ownerId(),
					rowChange.change(),
					changesetId
			);
		}

		String describe() {
			return "GraphAuditCollection(" + mutationSupport.getMutationTarget().getRolePath() + "#end)";
		}
	}

	record CollectionAuditRowChange(
			PersistentCollection<?> collection,
			Object ownerId,
			AuditCollectionChange change) {
	}

	private record EntityAuditInsertBindPlan(
			EntityAuditSupport mutationSupport,
			int tableIndex,
			Object id,
			Object[] values,
			boolean[] propertyInclusions,
			ModificationType modificationType,
			Object changesetId) implements BindPlan {

		@Override
		public Object getEntityId() {
			return id;
		}

		@Override
		public void bindValues(
				JdbcValueBindings valueBindings,
				FlushOperation flushOperation,
				SharedSessionContractImplementor session) {
			mutationSupport.bindAuditInsertValues(
					tableIndex,
					id,
					values,
					propertyInclusions,
					modificationType,
					changesetId,
					session,
					valueBindings
			);
		}
	}

	private record EntityTransactionEndBindPlan(
			EntityAuditSupport mutationSupport,
			int tableIndex,
			Object id,
			ModificationType modificationType,
			Object changesetId) implements BindPlan, OperationResultChecker {

		@Override
		public Object getEntityId() {
			return id;
		}

		@Override
		public void bindValues(
				JdbcValueBindings valueBindings,
				FlushOperation flushOperation,
				SharedSessionContractImplementor session) {
			mutationSupport.bindTransactionEndValues( tableIndex, id, changesetId, session, valueBindings );
		}

		@Override
		public boolean checkResult(
				int affectedRowCount,
				int batchPosition,
				String sqlString,
				org.hibernate.engine.spi.SessionFactoryImplementor sessionFactory) throws SQLException {
			return EntityAuditSupport.verifyTransactionEndOutcome(
					affectedRowCount,
					modificationType,
					mutationSupport.getEntityPersister().getEntityName(),
					id
			);
		}
	}

	private record CollectionAuditInsertBindPlan(
			CollectionAuditSupport mutationSupport,
			PersistentCollection<?> collection,
			Object ownerId,
			AuditCollectionChange change,
			Object changesetId) implements BindPlan {

		@Override
		public void bindValues(
				JdbcValueBindings valueBindings,
				FlushOperation flushOperation,
				SharedSessionContractImplementor session) {
			mutationSupport.bindAuditInsertValues( collection, ownerId, change, changesetId, session, valueBindings );
		}
	}

	private record CollectionTransactionEndBindPlan(
			CollectionAuditSupport mutationSupport,
			PersistentCollection<?> collection,
			Object ownerId,
			AuditCollectionChange change,
			Object changesetId) implements BindPlan, OperationResultChecker {

		@Override
		public void bindValues(
				JdbcValueBindings valueBindings,
				FlushOperation flushOperation,
				SharedSessionContractImplementor session) {
			mutationSupport.bindTransactionEndValues( collection, ownerId, change, changesetId, session, valueBindings );
		}

		@Override
		public boolean checkResult(
				int affectedRowCount,
				int batchPosition,
				String sqlString,
				org.hibernate.engine.spi.SessionFactoryImplementor sessionFactory) {
			return true;
		}
	}
}
