package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.sql.SQLException;

import org.hibernate.Internal;
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
@Internal
abstract class AbstractAuditCoordinator extends AbstractMutationCoordinator implements AuditWriter {
	protected final BasicBatchKey auditBatchKey;
	protected final boolean[] auditedPropertyMask;
	@Nullable
	private final MutationOperationGroup staticAuditInsertGroup;
	@Nullable
	private final MutationOperationGroup transactionEndUpdateGroup;
	private final EntityAuditSupport entityAuditSupport;

	protected AbstractAuditCoordinator(@Nonnull EntityPersister entityPersister, @Nonnull SessionFactoryImplementor factory) {
		super( entityPersister, factory );
		this.entityAuditSupport = new EntityAuditSupport( entityPersister, factory );
		this.auditedPropertyMask = entityAuditSupport.getAuditedPropertyMask();
		this.auditBatchKey = new BasicBatchKey( entityPersister.getEntityName() + "#AUDIT_INSERT" );
		final var staticAuditInsertMutationGroup = entityAuditSupport.getStaticAuditInsertMutationGroup();
		this.staticAuditInsertGroup = staticAuditInsertMutationGroup == null
				? null
				: createOperationGroup( null, staticAuditInsertMutationGroup );
		this.transactionEndUpdateGroup = entityAuditSupport.getTransactionEndUpdateMutationGroup() == null
				? null
				: createOperationGroup( null, entityAuditSupport.getTransactionEndUpdateMutationGroup() );
	}

	/**
	 * Enqueue an audit entry for deferred writing at transaction completion.
	 */
	protected void enqueueAuditEntry(
			@Nonnull EntityKey entityKey,
			@Nonnull Object entity,
			@Nonnull Object[] values,
			@Nonnull ModificationType modificationType,
			@Nonnull SharedSessionContractImplementor session) {
		session.getAuditWorkQueue().enqueue(
				entityKey,
				entity,
				values,
				modificationType,
				this,
				session
		);
	}

	@Nonnull
	protected EntityKey resolveEntityKey(@Nonnull Object entity, @Nonnull Object id, @Nonnull SharedSessionContractImplementor session) {
		final var entityEntry = session.getPersistenceContextInternal().getEntry( entity );
		return entityEntry != null
				? entityEntry.getEntityKey()
				: new EntityKey( id, entityPersister() );
	}

	/**
	 * Write an audit row, called by {@link org.hibernate.audit.spi.AuditWorkQueue}
	 * at transaction completion.
	 */
	@Override
	public void writeAuditRow(
			@Nonnull EntityKey entityKey,
			@Nonnull Object entity,
			@Nonnull Object[] values,
			@Nonnull ModificationType modificationType,
			@Nonnull SharedSessionContractImplementor session) {
		final var id = entityKey.getIdentifier();
		updatePreviousTransactionEnd( id, modificationType, session );

		final boolean dynamicInsert = entityPersister().isDynamicInsert();
		final boolean[] propertyInclusions = entityAuditSupport.resolvePropertyInclusions( entity, values, session );
		final var mutationGroup = dynamicInsert
				? entityAuditSupport.resolveAuditInsertMutationGroup( propertyInclusions, entity, session )
				: null;
		final MutationOperationGroup operationGroup = dynamicInsert
				? mutationGroup == null ? null : createOperationGroup( null, mutationGroup )
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

	@Nullable
	@Override
	protected BatchKey getBatchKey() {
		return auditBatchKey;
	}

	private void bindAuditValues(
			@Nonnull Object id,
			@Nonnull Object[] values,
			@Nonnull boolean[] propertyInclusions,
			@Nonnull ModificationType modificationType,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
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
			@Nonnull Object id,
			@Nonnull ModificationType modificationType,
			@Nonnull SharedSessionContractImplementor session) {
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
			@Nonnull PreparedStatementDetails statementDetails,
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
