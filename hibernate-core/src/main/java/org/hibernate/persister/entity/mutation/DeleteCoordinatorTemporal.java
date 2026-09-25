package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.persister.entity.mutation.AbstractTemporalUpdateCoordinator.applyTemporalEnding;

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

	public DeleteCoordinatorTemporal(@Nonnull EntityPersister entityPersister, @Nonnull SessionFactoryImplementor factory) {
		super( entityPersister, factory );
		this.temporalMapping = castNonNull( entityPersister.getTemporalMapping() );
	}

	@Override
	protected void applyStaticDeleteTableDetails(
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nullable Object[] loadedState,
			@Nullable Object version,
			boolean applyVersion,
			@Nonnull MutationExecutor mutationExecutor,
			@Nonnull SharedSessionContractImplementor session) {
		super.applyStaticDeleteTableDetails( id, rowId, loadedState, version, applyVersion, mutationExecutor, session );
		bindTemporalEndingValue( session, mutationExecutor.getJdbcValueBindings() );
	}

	@Override
	protected void applyDynamicDeleteTableDetails(
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nullable Object[] loadedState,
			@Nonnull MutationExecutor mutationExecutor,
			@Nonnull MutationOperationGroup operationGroup,
			@Nonnull SharedSessionContractImplementor session) {
		super.applyDynamicDeleteTableDetails( id, rowId, loadedState, mutationExecutor, operationGroup, session );
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
			boolean applyVersion,
			@Nullable SharedSessionContractImplementor session) {
		final var rootTableMapping = entityPersister().getIdentifierTableMapping();
		final var tableUpdateBuilder = new TableUpdateBuilderStandard<>( entityPersister(), rootTableMapping, factory() );

		applyKeyRestriction( rowId, entityPersister(), tableUpdateBuilder, rootTableMapping );
		applyTemporalEnding( tableUpdateBuilder, entityPersister().getTemporalMapping() );
		applyPartitionKeyRestriction( tableName -> tableUpdateBuilder );
		applyOptimisticLocking(
				entityPersister().optimisticLockStyle(),
				tableMutationBuilderResolver( tableUpdateBuilder ),
				loadedState,
				session
		);
		applyTenantRestriction( tableUpdateBuilder );

		return createMutationOperationGroup( tableUpdateBuilder );
	}
}
