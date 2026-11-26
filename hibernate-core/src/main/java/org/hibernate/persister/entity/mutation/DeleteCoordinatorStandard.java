package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.ast.spi.model.builder.MutationGroupBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableDeleteBuilder;
import org.hibernate.sql.ast.internal.model.builder.TableDeleteBuilderSkipped;
import org.hibernate.sql.ast.spi.model.builder.TableDeleteBuilderStandard;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Coordinates standard deleting of an entity.
 *
 * @author Steve Ebersole
 */
@org.hibernate.Internal
public class DeleteCoordinatorStandard extends AbstractDeleteCoordinator {
	private final Set<String> customDeleteTables;

	public DeleteCoordinatorStandard(@Nonnull EntityPersister entityPersister, @Nonnull SessionFactoryImplementor factory) {
		super( entityPersister, factory );
		customDeleteTables = new HashSet<>();
		if ( entityPersister.hasPartitionedSelectionMapping() ) {
			entityPersister.forEachMutableTable( tableMapping -> {
				if ( !tableMapping.isCascadeDeleteEnabled() && tableMapping.getDeleteDetails().getCustomSql() != null ) {
					customDeleteTables.add( tableMapping.getTableName() );
				}
			} );
		}
	}

	@Nonnull
	@Override
	protected List<PartitionKeyValue> resolvePartitionKeys(
			@Nonnull Object id,
			@Nullable Object[] state,
			@Nonnull SharedSessionContractImplementor session) {
		return PartitionKeyValue.forDelete( entityPersister(), id, state, customDeleteTables, session );
	}

	@Nonnull
	@Override
	protected MutationOperationGroup generateOperationGroup(
			@Nullable Object rowId,
			@Nullable Object[] loadedState,
			@Nullable List<PartitionKeyValue> partitionKeys,
			boolean applyVersion,
			@Nullable SharedSessionContractImplementor session) {
		final var deleteGroupBuilder = new MutationGroupBuilder( MutationType.DELETE, entityPersister() );

		entityPersister().forEachMutableTableReverse( tableMapping ->
				deleteGroupBuilder.addTableDetailsBuilder( tableMapping.isCascadeDeleteEnabled()
						? new TableDeleteBuilderSkipped( tableMapping )
						: createTableDeleteBuilder( tableMapping ) ) );

		applyTableDeleteDetails( deleteGroupBuilder, rowId, loadedState, partitionKeys, applyVersion, session );

		return createOperationGroup( null, deleteGroupBuilder.buildMutationGroup() );
	}

	@Nonnull
	TableDeleteBuilder createTableDeleteBuilder(@Nonnull EntityTableMapping tableMapping) {
		return new TableDeleteBuilderStandard( entityPersister(), tableMapping, factory() );
	}

	private void applyTableDeleteDetails(
			@Nonnull MutationGroupBuilder deleteGroupBuilder,
			@Nullable Object rowId,
			@Nullable Object[] loadedState,
			@Nullable List<PartitionKeyValue> partitionKeys,
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
			applyPartitionKeyRestriction( deleteGroupBuilder::findTableDetailsBuilder, partitionKeys,
					TableMapping::getDeleteDetails );
		}
		deleteGroupBuilder.forEachTableMutationBuilder( builder ->
				applyTenantRestriction( (TableDeleteBuilder) builder ) );
	}

}
