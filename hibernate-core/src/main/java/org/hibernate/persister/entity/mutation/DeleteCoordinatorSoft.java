package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.spi.mutation.TableMapping;

import java.util.List;

/**
 * DeleteCoordinator for soft-deletes
 *
 * @author Steve Ebersole
 */
@org.hibernate.Internal
public class DeleteCoordinatorSoft extends AbstractDeleteCoordinator {
	public DeleteCoordinatorSoft(@Nonnull EntityPersister entityPersister, @Nonnull SessionFactoryImplementor factory) {
		super( entityPersister, factory );
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
		applySoftDelete( entityPersister().getSoftDeleteMapping(), tableUpdateBuilder );
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

	private static void applySoftDelete(
			@Nonnull SoftDeleteMapping softDeleteMapping,
			@Nonnull TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		final var softDeleteColumnReference =
				new ColumnReference( tableUpdateBuilder.getMutatingTable(), softDeleteMapping );
		// apply the assignment
		tableUpdateBuilder.addValueColumn( softDeleteMapping.createDeletedValueBinding( softDeleteColumnReference ) );
		// apply the restriction
		tableUpdateBuilder.addNonKeyRestriction( softDeleteMapping.createNonDeletedValueBinding( softDeleteColumnReference ) );
	}
}
