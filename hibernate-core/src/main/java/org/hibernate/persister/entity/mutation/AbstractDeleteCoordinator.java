package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.internal.FilteredAssociationState;
import org.hibernate.engine.internal.TenantIdHelper;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.spi.mutation.TableMapping.MutationDetails;
import org.hibernate.sql.ast.spi.model.builder.RestrictedTableMutationBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import java.util.List;
import java.util.function.Function;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Template support for DeleteCoordinator implementations.  Mainly
 * centers around delegation via {@linkplain #generateOperationGroup}.
 *
 * @author Steve Ebersole
 */
@org.hibernate.Internal
public abstract class AbstractDeleteCoordinator
		extends AbstractMutationCoordinator
		implements DeleteCoordinator {
	private final BasicBatchKey batchKey;
	private final MutationOperationGroup staticOperationGroup;

	@Nullable
	private MutationOperationGroup noVersionDeleteGroup;

	public AbstractDeleteCoordinator(
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory) {
		super( entityPersister, factory );

		batchKey = new BasicBatchKey( entityPersister.getEntityName() + "#DELETE" );
		staticOperationGroup = generateOperationGroup( "", null, null, true, null );
		if ( !entityPersister.isVersioned() && !entityPersister.hasRowId() ) {
			noVersionDeleteGroup = staticOperationGroup;
		}
	}

	@Nullable
	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return staticOperationGroup;
	}

	@Nullable
	@Override
	public BasicBatchKey getBatchKey() {
		return batchKey;
	}

	/**
	 * @param partitionKeys the partition key of the row, or {@code null} for
	 *                      a statement with a parameter for each of its columns
	 */
	@Nonnull
	protected abstract MutationOperationGroup generateOperationGroup(
			@Nullable Object rowId,
			@Nullable Object[] loadedState,
			@Nullable List<PartitionKeyValue> partitionKeys,
			boolean applyVersion,
			@Nullable SharedSessionContractImplementor session);

	/**
	 * The partition key of the row, as far as it is known from the given state.
	 */
	@Nonnull
	protected List<PartitionKeyValue> resolvePartitionKeys(
			@Nonnull Object id,
			@Nullable Object[] state,
			@Nonnull SharedSessionContractImplementor session) {
		return PartitionKeyValue.fromState( entityPersister(), state, session );
	}

	@Override
	public void delete(
			@Nullable Object entity,
			@Nonnull Object id,
			@Nullable Object version,
			@Nonnull SharedSessionContractImplementor session) {
		TenantIdHelper.validateIdentifierTenant( id, entityPersister(), session );
		final boolean isImpliedOptimisticLocking = entityPersister().optimisticLockStyle().isAllOrDirty();

		final var entry = entity == null ? null : session.getPersistenceContextInternal().getEntry( entity );
		final var filteredState = entry == null ? null : entry.getExtraState( FilteredAssociationState.class );
		final Object[] originalLoadedState = entry == null ? null : entry.getLoadedState();
		final Object[] loadedState = filteredState == null ? originalLoadedState
				: filteredState.physicalState( originalLoadedState, entityPersister() );
		final Object rowId = entry != null ? entry.getRowId() : null;
		// An instance which does not belong to a persistence context has no loaded
		// state, so its current state is all that is known of its row. A read-only
		// instance has no loaded state either, but its current state might differ
		// from its row. An unloaded reference to an entity with a partition key is
		// always loaded before it is deleted.
		final List<PartitionKeyValue> partitionKeys = entity != null && entityPersister().hasPartitionedSelectionMapping()
				? resolvePartitionKeys( id, entry == null ? entityPersister().getValues( entity ) : loadedState, session )
				: List.of();

		if ( entity != null && ( isImpliedOptimisticLocking && loadedState != null
				|| rowId == null && entityPersister().hasRowId()
				|| PartitionKeyValue.needsDynamicRestriction( entityPersister(), partitionKeys ) ) ) {
			doDynamicDelete( entity, id, rowId, loadedState, partitionKeys, version, session );
		}
		else {
			doStaticDelete( entity, id, rowId, partitionKeys, version, session );
		}
	}

	protected void doDynamicDelete(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nullable Object[] loadedState,
			@Nonnull List<PartitionKeyValue> partitionKeys,
			@Nullable Object version,
			@Nonnull SharedSessionContractImplementor session) {
		final var operationGroup =
				generateOperationGroup( null, loadedState, partitionKeys, true, session );
		final var mutationExecutor = executor( session, operationGroup );

		for ( int i = 0; i < operationGroup.getNumberOfOperations(); i++ ) {
			final var mutation = operationGroup.getOperation( i );
			if ( mutation != null ) {
				final String tableName = mutation.getTableDetails().getTableName();
				mutationExecutor.getPreparedStatementDetails( tableName );
			}
		}

		applyDynamicDeleteTableDetails(
				id,
				rowId,
				loadedState,
				partitionKeys,
				version,
				mutationExecutor,
				operationGroup,
				session
		);

		try {
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

	protected void applyDynamicDeleteTableDetails(
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nullable Object[] loadedState,
			@Nonnull List<PartitionKeyValue> partitionKeys,
			@Nullable Object version,
			@Nonnull MutationExecutor mutationExecutor,
			@Nonnull MutationOperationGroup operationGroup,
			@Nonnull SharedSessionContractImplementor session) {
		applyLocking( version, loadedState, mutationExecutor, session );
		PartitionKeyValue.bindRestrictions( partitionKeys, mutationExecutor.getJdbcValueBindings(),
				operationGroup::getOperation );
		bindTenantRestriction( session, mutationExecutor.getJdbcValueBindings(), operationGroup );
		applyId( id, null, mutationExecutor, operationGroup, session );
	}

	protected void applyLocking(
			@Nullable Object version,
			@Nullable Object[] loadedState,
			@Nonnull MutationExecutor mutationExecutor,
			@Nonnull SharedSessionContractImplementor session) {
		final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
		switch ( entityPersister().optimisticLockStyle() ) {
			case VERSION:
				applyVersionLocking( version, jdbcValueBindings );
				break;
			case ALL:
			case DIRTY:
				applyAllOrDirtyLocking( loadedState, session, jdbcValueBindings );
				break;
		}
	}

	private void applyAllOrDirtyLocking(
			@Nullable Object[] loadedState,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
		if ( loadedState != null ) {
			final var persister = entityPersister();
			final boolean[] versionability = persister.getPropertyVersionability();
			for ( int attributeIndex = 0; attributeIndex < versionability.length; attributeIndex++ ) {
				// only makes sense to lock on singular attributes which are not excluded from optimistic locking
				if ( versionability[attributeIndex] ) {
					final var attribute = persister.getAttributeMapping( attributeIndex );
					if ( !attribute.isPluralAttributeMapping() ) {
						final Object loadedValue = loadedState[attributeIndex];
						if ( loadedValue != null ) {
							attribute.breakDownJdbcValues(
									loadedValue,
									0,
									jdbcValueBindings,
									persister.getAttributeMutationTableName( attributeIndex ),
									(valueIndex, bindings, tableName, jdbcValue, jdbcValueMapping) -> {
										if ( jdbcValue == null ) {
											// presumably the SQL was generated with `is null`
											return;
										}
										bindings.bindValue(
												jdbcValue,
												tableName,
												jdbcValueMapping.getSelectionExpression(),
												ParameterUsage.RESTRICT
										);
									},
									session
							);
						}
					}
				}
			}
		}
	}

	private void applyVersionLocking(
			@Nullable Object version,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
		final var persister = entityPersister();
		final var versionMapping = persister.getVersionMapping();
		if ( version != null && versionMapping != null ) {
			jdbcValueBindings.bindValue(
					version,
					persister.physicalTableNameForMutation( versionMapping ),
					versionMapping.getSelectionExpression(),
					ParameterUsage.RESTRICT
			);
		}
	}

	protected void applyId(
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nonnull MutationExecutor mutationExecutor,
			@Nonnull MutationOperationGroup operationGroup,
			@Nonnull SharedSessionContractImplementor session) {
		final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
//		final EntityRowIdMapping rowIdMapping = entityPersister().getRowIdMapping();
		for ( int position = 0; position < operationGroup.getNumberOfOperations(); position++ ) {
			final var jdbcMutation = operationGroup.getOperation( position );
			final var tableDetails = (EntityTableMapping) jdbcMutation.getTableDetails();
			breakDownKeyJdbcValues( id, rowId, session, jdbcValueBindings, tableDetails );
			final var statementDetails =
					mutationExecutor.getPreparedStatementDetails( tableDetails.getTableName() );
			if ( statementDetails != null ) {
				// force creation of the PreparedStatement
				//noinspection resource
				statementDetails.resolveStatement();
			}
		}
	}

	protected void doStaticDelete(
			@Nullable Object entity,
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nonnull List<PartitionKeyValue> partitionKeys,
			@Nullable Object version,
			@Nonnull SharedSessionContractImplementor session) {
		final boolean applyVersion;
		final MutationOperationGroup operationGroupToUse;
		if ( entity == null ) {
			applyVersion = false;
			operationGroupToUse = resolveNoVersionDeleteGroup( session );
		}
		else {
			applyVersion = true;
			operationGroupToUse = staticOperationGroup;
		}

		final var mutationExecutor = executor( session, operationGroupToUse );
		for ( int position = 0; position < operationGroupToUse.getNumberOfOperations(); position++ ) {
			final var mutation = operationGroupToUse.getOperation( position );
			if ( mutation != null ) {
				mutationExecutor.getPreparedStatementDetails( mutation.getTableDetails().getTableName() );
			}
		}

		applyStaticDeleteTableDetails(
				id,
				rowId,
				partitionKeys,
				version,
				applyVersion,
				mutationExecutor,
				session
		);

		try {
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

	protected void applyStaticDeleteTableDetails(
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nonnull List<PartitionKeyValue> partitionKeys,
			@Nullable Object version,
			boolean applyVersion,
			@Nonnull MutationExecutor mutationExecutor,
			@Nonnull SharedSessionContractImplementor session) {
		if ( applyVersion ) {
			applyLocking( version, null, mutationExecutor, session );
		}

		final var operationGroup = applyVersion ? staticOperationGroup : resolveNoVersionDeleteGroup( session );
		PartitionKeyValue.bindRestrictions( partitionKeys, mutationExecutor.getJdbcValueBindings(),
				operationGroup::getOperation );
		bindTenantRestriction( session, mutationExecutor.getJdbcValueBindings(), operationGroup );
		applyId( id, rowId, mutationExecutor, staticOperationGroup, session );
	}

	@Nonnull
	private MutationExecutor executor(@Nonnull SharedSessionContractImplementor session, @Nonnull MutationOperationGroup group) {
		return mutationExecutorService.createExecutor( resolveBatchKeyAccess( group != staticOperationGroup, session ),
				group, session );
	}

	@Nonnull
	protected MutationOperationGroup resolveNoVersionDeleteGroup(@Nonnull SharedSessionContractImplementor session) {
		if ( noVersionDeleteGroup == null ) {
			noVersionDeleteGroup = generateOperationGroup( null, null, null, false, session );
		}
		return noVersionDeleteGroup;
	}

	protected void applyOptimisticLocking(
			@Nonnull OptimisticLockStyle optimisticLockStyle,
			@Nonnull Function<String,RestrictedTableMutationBuilder<?, ?>> resolver,
			@Nullable Object[] loadedState,
			@Nullable SharedSessionContractImplementor session) {
		if ( optimisticLockStyle.isVersion() && entityPersister().getVersionMapping() != null ) {
			applyVersionBasedOptLocking( resolver );
		}
		else if ( loadedState != null && optimisticLockStyle.isAllOrDirty() ) {
			applyNonVersionOptLocking( optimisticLockStyle, resolver, loadedState, castNonNull( session ) );
		}
	}

	protected void applyVersionBasedOptLocking(@Nonnull Function<String,RestrictedTableMutationBuilder<?, ?>> resolver) {
		final var versionMapping = entityPersister().getVersionMapping();
		if ( versionMapping != null ) {
			final String tableNameForMutation =
					entityPersister().physicalTableNameForMutation( versionMapping );
			final var tableMutationBuilder = resolver.apply( tableNameForMutation );
			if ( tableMutationBuilder != null ) {
				applyVersionOptimisticLocking( tableMutationBuilder );
			}
		}
	}

	protected void applyNonVersionOptLocking(
			@Nonnull OptimisticLockStyle lockStyle,
			@Nonnull Function<String,RestrictedTableMutationBuilder<?, ?>> resolver,
			@Nonnull Object[] loadedState,
			@Nonnull SharedSessionContractImplementor session) {
		final var persister = entityPersister();
		assert loadedState != null;
		assert lockStyle.isAllOrDirty();
		assert persister.optimisticLockStyle().isAllOrDirty();
		assert session != null;

		final boolean[] versionability = persister.getPropertyVersionability();
		for ( int attributeIndex = 0; attributeIndex < versionability.length; attributeIndex++ ) {
			// only makes sense to lock on singular attributes which are not excluded from optimistic locking
			if ( versionability[attributeIndex] ) {
				final var attribute = persister.getAttributeMapping( attributeIndex );
				if ( !attribute.isPluralAttributeMapping() ) {
					breakDownJdbcValues( resolver, session, attribute, loadedState[attributeIndex] );
				}
			}
		}
	}

	private void breakDownJdbcValues(
			@Nonnull Function<String,RestrictedTableMutationBuilder<?, ?>> resolver,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull AttributeMapping attribute,
			@Nullable Object loadedValue) {
		final var tableMutationBuilder =
				resolver.apply( attribute.getContainingTableExpression() );
		if ( tableMutationBuilder != null ) {
			final var optimisticLockBindings = tableMutationBuilder.getOptimisticLockBindings();
			if ( optimisticLockBindings != null ) {
				attribute.breakDownJdbcValues(
						loadedValue,
						(valueIndex, value, jdbcValueMapping) -> {
							if ( !tableMutationBuilder.getKeyRestrictionBindings()
									.containsColumn(
											jdbcValueMapping.getSelectableName(),
											jdbcValueMapping.getJdbcMapping()
									) ) {
								optimisticLockBindings.consume( valueIndex, value, jdbcValueMapping );
							}
						},
						session
				);
			}
		}
	}

	@Nonnull
	protected Function<String,RestrictedTableMutationBuilder<?, ?>> tableMutationBuilderResolver(
			@Nonnull RestrictedTableMutationBuilder<?, ?> tableMutationBuilder) {
		final String tableName = tableMutationBuilder.getMutatingTable().getTableName();
		return name -> tableName.equals( name ) ? tableMutationBuilder : null;
	}

	/**
	 * @param partitionKeys the partition key of the row, or {@code null} for
	 *                      a parameter for each of its columns
	 */
	protected void applyPartitionKeyRestriction(
			@Nonnull Function<String,RestrictedTableMutationBuilder<?, ?>> resolver,
			@Nullable List<PartitionKeyValue> partitionKeys,
			@Nonnull Function<TableMapping, MutationDetails> mutationDetails) {
		final var persister = entityPersister();
		if ( persister.hasPartitionedSelectionMapping() ) {
			PartitionKeyValue.addRestrictions( persister, resolver, partitionKeys, mutationDetails );
		}
	}

	@Nonnull
	MutationOperationGroup createMutationOperationGroup(@Nonnull TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		final var tableMutation = tableUpdateBuilder.buildMutation();
		return singleOperation(
				new MutationGroupSingle( MutationType.DELETE, entityPersister(), tableMutation ),
				tableMutation.createMutationOperation( null, factory() )
		);
	}
}
