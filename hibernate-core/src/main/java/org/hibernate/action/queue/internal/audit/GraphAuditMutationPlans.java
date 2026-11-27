package org.hibernate.action.queue.internal.audit;

import java.sql.PreparedStatement;

import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.bind.OperationResultChecker;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.mutation.CollectionAuditSupport;
import org.hibernate.persister.collection.mutation.CollectionAuditSupport.AuditCollectionChange;

/// Graph-audit helpers used while materializing merged audit changes as
/// ordinary [FlushOperation] instances.
///
/// The collector owns creation of [FlushOperation] instances. These helpers keep
/// the queue-local bind plans, result checking, and operation descriptions out of
/// the collector's change-set orchestration code.
final class GraphAuditMutationPlans {
	private GraphAuditMutationPlans() {
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
				PreparedStatement statement,
				int batchPosition,
				String sqlString,
				org.hibernate.engine.spi.SessionFactoryImplementor sessionFactory) {
			return true;
		}
	}
}
