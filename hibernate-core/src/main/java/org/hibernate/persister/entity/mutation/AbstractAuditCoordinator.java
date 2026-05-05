/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import java.sql.SQLException;

import org.hibernate.audit.ModificationType;
import org.hibernate.audit.spi.AuditWriter;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;

/**
 * Base support for audit log insert coordinators.
 * <p>
 * Supports all inheritance strategies: for SINGLE_TABLE / TABLE_PER_CLASS
 * there is one audit table; for JOINED there is one per entity table.
 * The static operation group is cached for reuse.
 */
abstract class AbstractAuditCoordinator extends AbstractMutationCoordinator implements AuditWriter {
	protected final BasicBatchKey auditBatchKey;
	protected final boolean[] auditedPropertyMask;
	private final MutationOperationGroup staticAuditInsertGroup;
	private final MutationOperationGroup transactionEndUpdateGroup;
	private final EntityAuditSupport entityAuditSupport;

	protected AbstractAuditCoordinator(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );
		this.entityAuditSupport = new EntityAuditSupport( entityPersister, factory );
		this.auditedPropertyMask = entityAuditSupport.getAuditedPropertyMask();
		this.auditBatchKey = new BasicBatchKey( entityPersister.getEntityName() + "#AUDIT_INSERT" );
		this.staticAuditInsertGroup = entityPersister.isDynamicInsert()
				? null
				: createOperationGroup( null, entityAuditSupport.getStaticAuditInsertMutationGroup() );
		this.transactionEndUpdateGroup = entityAuditSupport.getTransactionEndUpdateMutationGroup() == null
				? null
				: createOperationGroup( null, entityAuditSupport.getTransactionEndUpdateMutationGroup() );
	}

	/**
	 * Enqueue an audit entry for deferred writing at transaction completion.
	 */
	protected void enqueueAuditEntry(
			Object entity,
			Object[] values,
			ModificationType modificationType,
			SharedSessionContractImplementor session) {
		final var entityEntry = session.getPersistenceContextInternal().getEntry( entity );
		session.getAuditWorkQueue().enqueue(
				entityEntry.getEntityKey(),
				entity,
				values,
				modificationType,
				this,
				session
		);
	}

	/**
	 * Write an audit row, called by {@link org.hibernate.audit.spi.AuditWorkQueue}
	 * at transaction completion.
	 */
	@Override
	public void writeAuditRow(
			EntityKey entityKey,
			Object entity,
			Object[] values,
			ModificationType modificationType,
			SharedSessionContractImplementor session) {
		final var id = entityKey.getIdentifier();
		updatePreviousTransactionEnd( id, modificationType, session );

		final boolean dynamicInsert = entityPersister().isDynamicInsert();
		final boolean[] propertyInclusions = entityAuditSupport.resolvePropertyInclusions( entity, values, session );
		final MutationOperationGroup operationGroup = dynamicInsert
				? createOperationGroup(
						null,
						entityAuditSupport.resolveAuditInsertMutationGroup( propertyInclusions, entity, session )
				)
				: staticAuditInsertGroup;
		if ( operationGroup == null ) {
			return;
		}

		final var mutationExecutor = mutationExecutorService.createExecutor(
				resolveBatchKeyAccess( dynamicInsert, session ),
				operationGroup,
				session
		);
		try {
			bindAuditValues(
					id,
					values,
					propertyInclusions,
					modificationType,
					session,
					mutationExecutor.getJdbcValueBindings()
			);
			mutationExecutor.execute( entity, null, null, AbstractAuditCoordinator::verifyOutcome, session );
		}
		finally {
			mutationExecutor.release();
		}
	}

	@Override
	protected BatchKey getBatchKey() {
		return auditBatchKey;
	}

	private void bindAuditValues(
			Object id,
			Object[] values,
			boolean[] propertyInclusions,
			ModificationType modificationType,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		for ( int tableIndex = 0; tableIndex < entityPersister().getTableMappings().length; tableIndex++ ) {
			entityAuditSupport.bindAuditInsertValues(
					tableIndex,
					id,
					values,
					propertyInclusions,
					modificationType,
					session,
					jdbcValueBindings
			);
		}
	}

	/**
	 * Update the previous audit row's transaction end column for the validity strategy.
	 * Sets {@code REVEND = :currentTxId} on the row with
	 * {@code REVEND IS NULL} for the given entity ID.
	 * <p>
	 * Called before the new audit row INSERT, so the just-inserted row
	 * does not exist yet and there's no risk of self-update.
	 *
	 * @param id the entity identifier
	 * @param modificationType the modification type of the new audit row
	 * @param session the current session
	 */
	private void updatePreviousTransactionEnd(
			Object id,
			ModificationType modificationType,
			SharedSessionContractImplementor session) {
		if ( transactionEndUpdateGroup == null ) {
			return;
		}
		final var mutationExecutor = mutationExecutorService.createExecutor(
				() -> auditBatchKey,
				transactionEndUpdateGroup,
				session
		);
		try {
			final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
			for ( int tableIndex = 0; tableIndex < entityPersister().getTableMappings().length; tableIndex++ ) {
				entityAuditSupport.bindTransactionEndValues( tableIndex, id, session, jdbcValueBindings );
			}
			final String entityName = entityPersister().getEntityName();
			mutationExecutor.execute(
					null, null, null,
					(statementDetails, affectedRowCount, batchPosition) ->
							EntityAuditSupport.verifyTransactionEndOutcome(
									affectedRowCount,
									modificationType,
									entityName,
									id
							),
					session
			);
		}
		finally {
			mutationExecutor.release();
		}
	}

	private static boolean verifyOutcome(
			PreparedStatementDetails statementDetails,
			int affectedRowCount,
			int batchPosition) throws SQLException {
		statementDetails.getExpectation().verifyOutcome(
				affectedRowCount,
				statementDetails.getStatement(),
				batchPosition,
				statementDetails.getSqlString()
		);
		return true;
	}
}
