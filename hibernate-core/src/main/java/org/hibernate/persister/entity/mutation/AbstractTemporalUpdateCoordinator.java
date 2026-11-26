package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

import org.hibernate.Internal;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.OperationResultChecker;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import static org.hibernate.persister.entity.mutation.TemporalMutationHelper.applyTemporalEnding;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * @author Gavin King
 */
@Internal
abstract class AbstractTemporalUpdateCoordinator extends AbstractMutationCoordinator implements UpdateCoordinator {
	AbstractTemporalUpdateCoordinator(@Nonnull EntityPersister entityPersister, @Nonnull SessionFactoryImplementor factory) {
		super( entityPersister, factory );
	}

	@Nonnull
	MutationOperationGroup buildEndingUpdateGroup(@Nonnull EntityTableMapping tableMapping, @Nonnull TemporalMapping temporalMapping) {
		return buildEndingUpdateGroup( tableMapping, temporalMapping, null );
	}

	/**
	 * @param partitionKeys the partition key of the row, or {@code null} for
	 *                      a parameter for each of its columns
	 */
	@Nonnull
	private MutationOperationGroup buildEndingUpdateGroup(
			@Nonnull EntityTableMapping tableMapping,
			@Nonnull TemporalMapping temporalMapping,
			@Nullable List<PartitionKeyValue> partitionKeys) {
		final var tableUpdateBuilder =
				new TableUpdateBuilderStandard<>( entityPersister(), tableMapping, factory() );

		applyKeyRestriction( null, entityPersister(), tableUpdateBuilder, tableMapping );
		applyTemporalEnding( tableUpdateBuilder, temporalMapping );
		// the table has the partition key columns of the identifier table
		applyPartitionKeyRestriction( tableUpdateBuilder, entityPersister().getIdentifierTableMapping().getTableName(),
				partitionKeys, TableMapping::getUpdateDetails );
		applyOptimisticLocking( tableUpdateBuilder );
		applyTenantRestriction( tableUpdateBuilder );

		return createMutationOperationGroup( tableUpdateBuilder );
	}

	@Nonnull
	MutationOperationGroup createMutationOperationGroup(@Nonnull TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		final var tableMutation = tableUpdateBuilder.buildMutation();
		return singleOperation(
				new MutationGroupSingle( MutationType.UPDATE, entityPersister(), tableMutation ),
				tableMutation.createMutationOperation( null, factory() )
		);
	}

	abstract void bindVersionRestriction(@Nullable Object oldVersion, @Nonnull JdbcValueBindings jdbcValueBindings, @Nonnull String temporalTableName);

	void registerUpdatedRevision(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session) {
		if ( !session.isStateless() || session.getTransactionCoordinator().isTransactionActive() ) {
			session.getPersistenceContextInternal()
					.registerUpdatedTemporalRevision( session.generateEntityKey( id, entityPersister() ) );
		}
	}

	/**
	 * @param state the state of the row which is ended, from which its partition key
	 *              is taken: its loaded state, or its current state if there is no
	 *              snapshot, as in a stateless session
	 */
	void performRowEndUpdate(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nullable Object oldVersion,
			@Nullable Object[] state,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull TemporalMapping temporalMapping,
			@Nonnull MutationOperationGroup staticEndUpdateGroup,
			@Nonnull String temporalTableName,
			@Nonnull OperationResultChecker resultChecker) {
		final var persister = entityPersister();
		final var partitionKeys = PartitionKeyValue.fromState( persister, state, session );
		// a null or unknown partition key cannot be bound to the static statement
		final boolean dynamicPartitionKey = PartitionKeyValue.needsDynamicRestriction( persister, partitionKeys );
		final var endUpdateGroup = dynamicPartitionKey
				? buildEndingUpdateGroup(
						(EntityTableMapping) staticEndUpdateGroup.getSingleOperation().getTableDetails(),
						temporalMapping, partitionKeys )
				: staticEndUpdateGroup;
		final var mutationExecutor =
				mutationExecutorService.createExecutor( resolveBatchKeyAccess( dynamicPartitionKey, session ),
						endUpdateGroup, session );
		try {
			final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
			bindTenantRestriction( session, jdbcValueBindings, endUpdateGroup );
			for ( int i = 0; i < endUpdateGroup.getNumberOfOperations(); i++ ) {
				breakDownKeyJdbcValues( id, rowId, session, jdbcValueBindings,
						(EntityTableMapping) endUpdateGroup.getOperation( i ).getTableDetails() );
			}

			bindVersionRestriction( oldVersion, jdbcValueBindings, temporalTableName );
			PartitionKeyValue.bindRestrictions( partitionKeys, persister.getIdentifierTableMapping().getTableName(),
					jdbcValueBindings, endUpdateGroup.getSingleOperation() );

			if ( TemporalMutationHelper.isUsingParameters( session ) ) {
				jdbcValueBindings.bindValue(
						session.getCurrentChangesetIdentifier(),
						temporalTableName,
						temporalMapping.getEndingColumnMapping().getSelectionExpression(),
						ParameterUsage.SET
				);
			}

			mutationExecutor.execute(
					entity,
					null,
					null,
					resultChecker,
					session,
					staleStateException -> staleObjectStateException( id, staleStateException )
			);
		}
		finally {
			mutationExecutor.release();
		}
	}
}
