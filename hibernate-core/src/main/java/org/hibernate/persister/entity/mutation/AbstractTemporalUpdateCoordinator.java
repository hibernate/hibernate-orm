package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.OperationResultChecker;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * @author Gavin King
 */
@Internal
abstract class AbstractTemporalUpdateCoordinator extends AbstractMutationCoordinator implements UpdateCoordinator {
	AbstractTemporalUpdateCoordinator(@Nonnull EntityPersister entityPersister, @Nonnull SessionFactoryImplementor factory) {
		super( entityPersister, factory );
	}

	static void applyTemporalEnding(@Nonnull TableUpdateBuilder<?> tableUpdateBuilder, @Nonnull TemporalMapping temporalMapping) {
		final var endingColumnReference =
				new ColumnReference( tableUpdateBuilder.getMutatingTable(), temporalMapping.getEndingColumnMapping() );
		tableUpdateBuilder.addValueColumn( temporalMapping.createEndingValueBinding( endingColumnReference ) );
		tableUpdateBuilder.addNonKeyRestriction( temporalMapping.createNullEndingValueBinding( endingColumnReference ) );
	}

	@Nonnull
	MutationOperationGroup buildEndingUpdateGroup(@Nonnull EntityTableMapping tableMapping, @Nonnull TemporalMapping temporalMapping) {
		final var tableUpdateBuilder =
				new TableUpdateBuilderStandard<>( entityPersister(), tableMapping, factory() );

		applyKeyRestriction( null, entityPersister(), tableUpdateBuilder, tableMapping );
		applyTemporalEnding( tableUpdateBuilder, temporalMapping );
		applyPartitionKeyRestriction( tableUpdateBuilder );
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

	void performRowEndUpdate(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nullable Object oldVersion,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull TemporalMapping temporalMapping,
			@Nonnull MutationOperationGroup endUpdateGroup,
			@Nonnull String temporalTableName,
			@Nonnull OperationResultChecker resultChecker) {
		final var mutationExecutor =
				mutationExecutorService.createExecutor( resolveBatchKeyAccess( false, session ),
						endUpdateGroup, session );
		try {
			final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
			bindTenantRestriction( session, jdbcValueBindings, endUpdateGroup );
			for ( int i = 0; i < endUpdateGroup.getNumberOfOperations(); i++ ) {
				breakDownKeyJdbcValues( id, rowId, session, jdbcValueBindings,
						(EntityTableMapping) endUpdateGroup.getOperation( i ).getTableDetails() );
			}

			bindVersionRestriction( oldVersion, jdbcValueBindings, temporalTableName );

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
