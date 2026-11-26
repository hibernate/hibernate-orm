package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilder;
import org.hibernate.sql.model.MutationOperationGroup;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.persister.entity.mutation.TemporalMutationHelper.isRevisionFromCurrentTransaction;

/**
 * Update coordinator for
 * {@link org.hibernate.temporal.TemporalTableStrategy#SINGLE_TABLE}
 * temporal strategy.
 *
 * @author Gavin King
 */
@Internal
public class UpdateCoordinatorTemporal extends AbstractTemporalUpdateCoordinator {
	private final TemporalMapping temporalMapping;
	private final MutationOperationGroup endingUpdateGroup;
	private final BasicBatchKey batchKey;
	private final UpdateCoordinator inPlaceUpdateCoordinator;

	public UpdateCoordinatorTemporal(
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory) {
		super( entityPersister, factory );
		this.temporalMapping = castNonNull( entityPersister.getTemporalMapping() );
		this.endingUpdateGroup = buildEndingUpdateGroup( entityPersister.getIdentifierTableMapping(), temporalMapping );
		this.batchKey = new BasicBatchKey( entityPersister.getEntityName() + "#TEMPORAL_UPDATE" );
		this.inPlaceUpdateCoordinator = new UpdateCoordinatorStandard( entityPersister, factory ) {
			@Override
			void applyTableUpdateRestrictions(@Nonnull TableUpdateBuilder<?> builder) {
				super.applyTableUpdateRestrictions( builder );
				final var ending = temporalMapping.getEndingColumnMapping();
				if ( builder.getMutatingTable().getTableName().equals( entityPersister.physicalTableNameForMutation( ending ) ) ) {
					builder.addNullRestriction( ending );
				}
			}
		};
	}

	@Nullable
	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return endingUpdateGroup;
	}

	@Nullable
	@Override
	protected BasicBatchKey getBatchKey() {
		return batchKey;
	}

	@Nullable
	@Override
	public GeneratedValues update(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nonnull Object[] values,
			@Nullable Object oldVersion,
			@Nullable Object[] incomingOldValues,
			@Nullable int[] dirtyAttributeIndexes,
			boolean hasDirtyCollection,
			@Nonnull SharedSessionContractImplementor session) {
		if ( isRevisionFromCurrentTransaction( entityPersister(), id, session )
				|| entityPersister().excludedFromTemporalVersioning( dirtyAttributeIndexes, hasDirtyCollection ) ) {
			return inPlaceUpdateCoordinator.update(
					entity,
					id,
					rowId,
					values,
					oldVersion,
					incomingOldValues,
					dirtyAttributeIndexes,
					hasDirtyCollection,
					session
			);
		}
		else {
			performRowEndUpdate(
					entity,
					id,
					null,
					oldVersion,
					incomingOldValues == null ? values : incomingOldValues,
					session,
					temporalMapping,
					endingUpdateGroup,
					entityPersister()
							.physicalTableNameForMutation( temporalMapping.getEndingColumnMapping() ),
					(statementDetails, affectedRowCount, batchPosition) ->
							resultCheck( id, statementDetails, affectedRowCount, batchPosition )

			);
			final var generatedValues = entityPersister().getInsertCoordinator().insert( entity, id, values, session );
			final var rowIdMapping = entityPersister().getRowIdMapping();
			if ( rowIdMapping != null && !session.isStateless() ) {
				// The replacement revision has a different physical row id.
				session.getPersistenceContextInternal().replaceEntityEntryRowId( entity,
						generatedValues == null ? null : generatedValues.getGeneratedValue( rowIdMapping ) );
			}
			registerUpdatedRevision( id, session );
			return generatedValues;
		}
	}

	@Override
	void bindVersionRestriction(@Nullable Object oldVersion, @Nonnull JdbcValueBindings jdbcValueBindings, @Nonnull String temporalTableName) {
		final var versionMapping = entityPersister().getVersionMapping();
		if ( versionMapping != null && entityPersister().optimisticLockStyle().isVersion() ) {
			jdbcValueBindings.bindValue( oldVersion, versionMapping, ParameterUsage.RESTRICT );
		}
	}

	@Override
	public void forceVersionIncrement(
			@Nonnull Object id,
			@Nullable Object currentVersion,
			@Nonnull Object nextVersion,
			@Nonnull SharedSessionContractImplementor session) {
		inPlaceUpdateCoordinator.forceVersionIncrement( id, currentVersion, nextVersion, session );
	}
}
