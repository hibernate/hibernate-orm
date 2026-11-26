package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import java.util.List;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.persister.entity.mutation.TemporalMutationHelper.createRevisionEndBuilder;
import static org.hibernate.persister.entity.mutation.TemporalMutationHelper.isRevisionFromCurrentTransaction;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Delete coordinator for
 * {@link org.hibernate.temporal.TemporalTableStrategy#HISTORY_TABLE}
 * temporal strategy.
 *
 * @author Gavin King
 */
@org.hibernate.Internal
public class DeleteCoordinatorHistory
		extends AbstractMutationCoordinator
		implements DeleteCoordinator {

	private final DeleteCoordinator currentDeleteCoordinator;
	private final EntityTableMapping historyTableMapping;
	private final TemporalMapping temporalMapping;
	private final BasicBatchKey historyBatchKey;
	private final MutationOperationGroup historyEndUpdateGroup;
	private final MutationOperationGroup unversionedHistoryEndUpdateGroup;

	public DeleteCoordinatorHistory(
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory,
			@Nonnull DeleteCoordinator currentDeleteCoordinator) {
		super( entityPersister, factory );
		this.currentDeleteCoordinator = currentDeleteCoordinator;
		this.temporalMapping = castNonNull( entityPersister.getTemporalMapping() );
		this.historyTableMapping =
				createAuxiliaryTableMapping( entityPersister.getIdentifierTableMapping(),
						entityPersister, temporalMapping.getTableName() );
		this.historyBatchKey = new BasicBatchKey( entityPersister.getEntityName() + "#HISTORY_DELETE" );
		final boolean versioned = entityPersister.optimisticLockStyle().isVersion()
				&& entityPersister.getVersionMapping() != null;
		this.historyEndUpdateGroup = buildHistoryEndGroup( MutationType.UPDATE, versioned, null );
		this.unversionedHistoryEndUpdateGroup =
				versioned ? buildHistoryEndGroup( MutationType.UPDATE, false, null ) : historyEndUpdateGroup;
	}

	@Nullable
	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return currentDeleteCoordinator.getStaticMutationOperationGroup();
	}

	@Nullable
	@Override
	protected BatchKey getBatchKey() {
		return historyBatchKey;
	}

	@Override
	public void delete(
			@Nullable Object entity,
			@Nonnull Object id,
			@Nullable Object version,
			@Nonnull SharedSessionContractImplementor session) {
		currentDeleteCoordinator.delete( entity, id, version, session );
		endHistoryRevision( entity, id, version, session );
	}

	private void endHistoryRevision(
			@Nullable Object entity,
			@Nonnull Object id,
			@Nullable Object oldVersion,
			@Nonnull SharedSessionContractImplementor session) {
		final var persister = entityPersister();
		final var versionMapping = persister.getVersionMapping();
		final boolean applyVersion =
				entity != null && versionMapping != null && persister.optimisticLockStyle().isVersion();
		final var partitionKeys = partitionKeys( entity, session );
		final var type = isRevisionFromCurrentTransaction( persister, id, session ) ? MutationType.DELETE : MutationType.UPDATE;
		final MutationOperationGroup operationGroup;
		if ( type == MutationType.DELETE || PartitionKeyValue.needsDynamicRestriction( persister, partitionKeys ) ) {
			operationGroup = buildHistoryEndGroup( type, applyVersion, partitionKeys );
		}
		else {
			operationGroup = entity == null ? unversionedHistoryEndUpdateGroup : historyEndUpdateGroup;
		}
		final var mutationExecutor =
				mutationExecutorService.createExecutor( resolveBatchKeyAccess( operationGroup != historyEndUpdateGroup, session ),
						operationGroup, session );
		try {
			final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
			for ( int i = 0; i < operationGroup.getNumberOfOperations(); i++ ) {
				final var operation = operationGroup.getOperation( i );
				breakDownKeyJdbcValues( id, null, session, jdbcValueBindings,
						(EntityTableMapping) operation.getTableDetails() );
			}

			PartitionKeyValue.bindRestrictions( partitionKeys, persister.getIdentifierTableMapping().getTableName(),
					jdbcValueBindings, operationGroup.getSingleOperation() );

			if ( applyVersion ) {
				jdbcValueBindings.bindValue(
						oldVersion,
						historyTableMapping.getTableName(),
						versionMapping.getSelectionExpression(),
						ParameterUsage.RESTRICT
				);
			}

			if ( type == MutationType.UPDATE && TemporalMutationHelper.isUsingParameters( session ) ) {
				jdbcValueBindings.bindValue(
						session.getCurrentChangesetIdentifier(),
						historyTableMapping.getTableName(),
						temporalMapping.getEndingColumnMapping().getSelectionExpression(),
						ParameterUsage.SET
				);
			}

			mutationExecutor.execute(
					entity,
					null,
					null,
					(statementDetails, affectedRowCount, batchPosition) ->
							resultCheck( id, statementDetails, affectedRowCount, batchPosition ),
					session,
					staleStateException -> staleObjectStateException( id, staleStateException )
			);
		}
		finally {
			mutationExecutor.release();
		}
	}

	/**
	 * The partition key of the row, taken from its loaded state, or from the given
	 * instance if it does not belong to a persistence context, as in a stateless
	 * session. An unloaded reference to an entity with a partition key is always
	 * loaded before it is deleted.
	 */
	@Nonnull
	private List<PartitionKeyValue> partitionKeys(
			@Nullable Object entity,
			@Nonnull SharedSessionContractImplementor session) {
		final var persister = entityPersister();
		if ( entity == null || !persister.hasPartitionedSelectionMapping() ) {
			return List.of();
		}
		final var entry = session.getPersistenceContextInternal().getEntry( entity );
		return PartitionKeyValue.fromState( persister,
				entry == null ? persister.getValues( entity ) : entry.getLoadedState(), session );
	}

	/**
	 * @param partitionKeys the partition key of the row, or {@code null} for
	 *                      a parameter for each of its columns
	 */
	@Nonnull
	private MutationOperationGroup buildHistoryEndGroup(
			@Nonnull MutationType type,
			boolean applyVersion,
			@Nullable List<PartitionKeyValue> partitionKeys) {
		final var builder = createRevisionEndBuilder( entityPersister(), historyTableMapping, type );
		applyKeyRestriction( null, entityPersister(), builder, historyTableMapping );
		// the history table has the partition key columns of the identifier table
		applyPartitionKeyRestriction( builder,
				entityPersister().getIdentifierTableMapping().getTableName(),
				partitionKeys, TableMapping::getUpdateDetails );
		if ( applyVersion ) {
			applyVersionOptimisticLocking( builder );
		}

		final var tableMutation = builder.buildMutation();
		return singleOperation(
				new MutationGroupSingle( MutationType.DELETE, entityPersister(), tableMutation ),
				tableMutation.createMutationOperation( null, factory() )
		);
	}
}
