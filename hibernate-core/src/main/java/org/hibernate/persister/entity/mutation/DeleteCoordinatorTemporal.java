package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.ast.spi.model.builder.TableDeleteBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.spi.mutation.TableMapping;

import java.util.List;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.persister.entity.mutation.TemporalMutationHelper.applyTemporalEnding;
import static org.hibernate.persister.entity.mutation.TemporalMutationHelper.createCurrentRevisionDeleteBuilder;
import static org.hibernate.persister.entity.mutation.TemporalMutationHelper.isRevisionFromCurrentTransaction;

/**
 * Delete coordinator for
 * {@link org.hibernate.temporal.TemporalTableStrategy#SINGLE_TABLE}
 * temporal strategy.
 *
 * @author Gavin King
 */
@org.hibernate.Internal
public class DeleteCoordinatorTemporal extends AbstractDeleteCoordinator {
	private final TemporalMapping temporalMapping;
	private final DeleteCoordinator currentRevisionDelete;

	public DeleteCoordinatorTemporal(@Nonnull EntityPersister entityPersister, @Nonnull SessionFactoryImplementor factory) {
		super( entityPersister, factory );
		this.temporalMapping = castNonNull( entityPersister.getTemporalMapping() );
		this.currentRevisionDelete = new DeleteCoordinatorStandard( entityPersister, factory ) {
			private final BasicBatchKey deleteKey = new BasicBatchKey( entityPersister.getEntityName() + "#TEMPORAL_DELETE" );

			@Override
			@Nonnull
			TableDeleteBuilder createTableDeleteBuilder(@Nonnull EntityTableMapping tableMapping) {
				return tableMapping.isIdentifierTable()
						? createCurrentRevisionDeleteBuilder( entityPersister(), tableMapping )
						: super.createTableDeleteBuilder( tableMapping );
			}

			@Override
			public BasicBatchKey getBatchKey() {
				return deleteKey;
			}
		};
	}

	@Override
	public void delete(
			@Nullable Object entity,
			@Nonnull Object id,
			@Nullable Object version,
			@Nonnull SharedSessionContractImplementor session) {
		if ( isRevisionFromCurrentTransaction( entityPersister(), id, session ) ) {
			currentRevisionDelete.delete( entity, id, version, session );
		}
		else {
			super.delete( entity, id, version, session );
		}
	}

	@Override
	protected void applyStaticDeleteTableDetails(
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nonnull List<PartitionKeyValue> partitionKeys,
			@Nullable Object version,
			boolean applyVersion,
			@Nonnull MutationExecutor mutationExecutor,
			@Nonnull SharedSessionContractImplementor session) {
		super.applyStaticDeleteTableDetails( id, rowId, partitionKeys, version, applyVersion, mutationExecutor, session );
		bindTemporalEndingValue( session, mutationExecutor.getJdbcValueBindings() );
	}

	@Override
	protected void applyDynamicDeleteTableDetails(
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nullable Object[] loadedState,
			@Nonnull List<PartitionKeyValue> partitionKeys,
			@Nullable Object version,
			@Nonnull MutationExecutor mutationExecutor,
			@Nonnull MutationOperationGroup operationGroup,
			@Nonnull SharedSessionContractImplementor session) {
		super.applyDynamicDeleteTableDetails(
				id, rowId, loadedState, partitionKeys, version, mutationExecutor, operationGroup, session );
		bindTemporalEndingValue( session, mutationExecutor.getJdbcValueBindings() );
	}

	private void bindTemporalEndingValue(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
		if ( TemporalMutationHelper.isUsingParameters( session ) ) {
			jdbcValueBindings.bindValue(
					session.getCurrentChangesetIdentifier(),
					entityPersister().physicalTableNameForMutation( temporalMapping.getEndingColumnMapping() ),
					temporalMapping.getEndingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}

	@Nonnull
	@Override
	protected MutationOperationGroup generateOperationGroup(
			@Nullable Object rowId,
			@Nullable Object[] loadedState,
			@Nullable List<PartitionKeyValue> partitionKeys,
			boolean applyVersion,
			@Nullable SharedSessionContractImplementor session) {
		final var rootTableMapping = entityPersister().getIdentifierTableMapping();
		final var tableUpdateBuilder = new TableUpdateBuilderStandard<>( entityPersister(), rootTableMapping, factory() );

		applyKeyRestriction( rowId, entityPersister(), tableUpdateBuilder, rootTableMapping );
		applyTemporalEnding( tableUpdateBuilder, entityPersister().getTemporalMapping() );
		// only the partition key columns of the root table restrict the update
		applyPartitionKeyRestriction( tableMutationBuilderResolver( tableUpdateBuilder ), partitionKeys,
				TableMapping::getUpdateDetails );
		if ( applyVersion ) {
			applyOptimisticLocking(
					entityPersister().optimisticLockStyle(),
					tableMutationBuilderResolver( tableUpdateBuilder ),
					loadedState,
					session
			);
		}
		applyTenantRestriction( tableUpdateBuilder );

		return createMutationOperationGroup( tableUpdateBuilder );
	}
}
