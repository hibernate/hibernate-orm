package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.ast.spi.model.builder.MutationGroupBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableDeleteBuilder;
import org.hibernate.sql.ast.internal.model.builder.TableDeleteBuilderSkipped;
import org.hibernate.sql.ast.spi.model.builder.TableDeleteBuilderStandard;

/**
 * Coordinates standard deleting of an entity.
 *
 * @author Steve Ebersole
 */
@org.hibernate.Internal
public class DeleteCoordinatorStandard extends AbstractDeleteCoordinator {

	public DeleteCoordinatorStandard(@Nonnull EntityPersister entityPersister, @Nonnull SessionFactoryImplementor factory) {
		super( entityPersister, factory );
	}

	@Nonnull
	@Override
	protected MutationOperationGroup generateOperationGroup(
			@Nullable Object rowId,
			@Nullable Object[] loadedState,
			boolean applyVersion,
			@Nullable SharedSessionContractImplementor session) {
		final var deleteGroupBuilder = new MutationGroupBuilder( MutationType.DELETE, entityPersister() );

		entityPersister().forEachMutableTableReverse( tableMapping ->
				deleteGroupBuilder.addTableDetailsBuilder( tableMapping.isCascadeDeleteEnabled()
						? new TableDeleteBuilderSkipped( tableMapping )
						: new TableDeleteBuilderStandard( entityPersister(), tableMapping, factory() ) ) );

		applyTableDeleteDetails( deleteGroupBuilder, rowId, loadedState, applyVersion, session );

		return createOperationGroup( null, deleteGroupBuilder.buildMutationGroup() );
	}

	private void applyTableDeleteDetails(
			@Nonnull MutationGroupBuilder deleteGroupBuilder,
			@Nullable Object rowId,
			@Nullable Object[] loadedState,
			boolean applyVersion,
			@Nullable SharedSessionContractImplementor session) {
		// first, the table key column(s)
		deleteGroupBuilder.forEachTableMutationBuilder( builder ->
				applyKeyRestriction( rowId, entityPersister(), (TableDeleteBuilder) builder,
						(EntityTableMappingImpl) builder.getMutatingTable().getTableMapping() ) );

		if ( applyVersion ) {
			// apply any optimistic locking
			applyOptimisticLocking(
					entityPersister().optimisticLockStyle(),
					deleteGroupBuilder::findTableDetailsBuilder,
					loadedState,
					session
			);
			applyPartitionKeyRestriction( deleteGroupBuilder::findTableDetailsBuilder );
		}
		deleteGroupBuilder.forEachTableMutationBuilder( builder ->
				applyTenantRestriction( (TableDeleteBuilder) builder ) );
	}

}
