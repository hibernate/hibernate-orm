/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.builder.RestrictedTableMutationBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.internal.MutationGroupSingle;

import java.util.function.Function;

import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;

/**
 * Template support for DeleteCoordinator implementations.  Mainly
 * centers around delegation via {@linkplain #generateOperationGroup}.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractDeleteCoordinator
		extends AbstractMutationCoordinator
		implements DeleteCoordinator {
	private final BasicBatchKey batchKey;
	private final MutationOperationGroup staticOperationGroup;

	private MutationOperationGroup noVersionDeleteGroup;

	public AbstractDeleteCoordinator(
			EntityPersister entityPersister,
			SessionFactoryImplementor factory) {
		super( entityPersister, factory );

		batchKey = new BasicBatchKey( entityPersister.getEntityName() + "#DELETE" );
		staticOperationGroup = generateOperationGroup( "", null, true, null );
		if ( !entityPersister.isVersioned() ) {
			noVersionDeleteGroup = staticOperationGroup;
		}
	}

	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return staticOperationGroup;
	}

	@Override
	public BasicBatchKey getBatchKey() {
		return batchKey;
	}

	protected abstract MutationOperationGroup generateOperationGroup(
			Object rowId,
			Object[] loadedState,
			boolean applyVersion,
			SharedSessionContractImplementor session);

	@Override
	public void delete(
			Object entity,
			Object id,
			Object version,
			SharedSessionContractImplementor session) {
		final boolean isImpliedOptimisticLocking = entityPersister().optimisticLockStyle().isAllOrDirty();

		final var entry = session.getPersistenceContextInternal().getEntry( entity );
		final var loadedState = entry != null && isImpliedOptimisticLocking ? entry.getLoadedState() : null;
		final Object rowId = entry != null ? entry.getRowId() : null;

		if ( isImpliedOptimisticLocking && loadedState != null || rowId == null && entityPersister().hasRowId() ) {
			doDynamicDelete( entity, id, rowId, loadedState, session );
		}
		else {
			doStaticDelete( entity, id, rowId, entry == null ? null : entry.getLoadedState(), version, session );
		}
	}

	protected void doDynamicDelete(
			Object entity,
			Object id,
			Object rowId,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
		final var operationGroup =
				generateOperationGroup( null, loadedState, true, session );
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
			Object id,
			Object rowId,
			Object[] loadedState,
			MutationExecutor mutationExecutor,
			MutationOperationGroup operationGroup,
			SharedSessionContractImplementor session) {
		applyLocking( null, loadedState, mutationExecutor, session );
		applyId( id, null, mutationExecutor, operationGroup, session );
	}

	protected void applyLocking(
			Object version,
			Object[] loadedState,
			MutationExecutor mutationExecutor,
			SharedSessionContractImplementor session) {
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
			Object[] loadedState,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
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
			Object version,
			JdbcValueBindings jdbcValueBindings) {
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
			Object id,
			Object rowId,
			MutationExecutor mutationExecutor,
			MutationOperationGroup operationGroup,
			SharedSessionContractImplementor session) {
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
			Object entity,
			Object id,
			Object rowId,
			Object[] loadedState,
			Object version,
			SharedSessionContractImplementor session) {
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
		for ( int position = 0; position < staticOperationGroup.getNumberOfOperations(); position++ ) {
			final var mutation = staticOperationGroup.getOperation( position );
			if ( mutation != null ) {
				mutationExecutor.getPreparedStatementDetails( mutation.getTableDetails().getTableName() );
			}
		}

		applyStaticDeleteTableDetails(
				id,
				rowId,
				loadedState,
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
			Object id,
			Object rowId,
			Object[] loadedState,
			Object version,
			boolean applyVersion,
			MutationExecutor mutationExecutor,
			SharedSessionContractImplementor session) {
		if ( applyVersion ) {
			applyLocking( version, null, mutationExecutor, session );
		}

		bindPartitionColumnValueBindings( loadedState, session,
				mutationExecutor.getJdbcValueBindings() );

		applyId( id, rowId, mutationExecutor, staticOperationGroup, session );
	}

	private MutationExecutor executor(SharedSessionContractImplementor session, MutationOperationGroup group) {
		return mutationExecutorService.createExecutor( resolveBatchKeyAccess( false, session ), group, session );
	}

	protected MutationOperationGroup resolveNoVersionDeleteGroup(SharedSessionContractImplementor session) {
		if ( noVersionDeleteGroup == null ) {
			noVersionDeleteGroup = generateOperationGroup( "", null, false, session );
		}
		return noVersionDeleteGroup;
	}

	protected void applyOptimisticLocking(
			OptimisticLockStyle optimisticLockStyle,
			Function<String,RestrictedTableMutationBuilder<?, ?>> resolver,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
		if ( optimisticLockStyle.isVersion() && entityPersister().getVersionMapping() != null ) {
			applyVersionBasedOptLocking( resolver );
		}
		else if ( loadedState != null && optimisticLockStyle.isAllOrDirty() ) {
			applyNonVersionOptLocking( optimisticLockStyle, resolver, loadedState, session );
		}
	}

	protected void applyVersionBasedOptLocking(Function<String,RestrictedTableMutationBuilder<?, ?>> resolver) {
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
			OptimisticLockStyle lockStyle,
			Function<String,RestrictedTableMutationBuilder<?, ?>> resolver,
			Object[] loadedState,
			SharedSessionContractImplementor session) {
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
			Function<String,RestrictedTableMutationBuilder<?, ?>> resolver,
			SharedSessionContractImplementor session,
			AttributeMapping attribute,
			Object loadedValue) {
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

	protected Function<String,RestrictedTableMutationBuilder<?, ?>> tableMutationBuilderResolver(
			RestrictedTableMutationBuilder<?, ?> tableMutationBuilder) {
		final String tableName = tableMutationBuilder.getMutatingTable().getTableName();
		return name -> tableName.equals( name ) ? tableMutationBuilder : null;
	}

	protected void applyPartitionKeyRestriction(Function<String,RestrictedTableMutationBuilder<?, ?>> resolver) {
		final var persister = entityPersister();
		if ( persister.hasPartitionedSelectionMapping() ) {
			final var attributeMappings = persister.getAttributeMappings();
			for ( int m = 0; m < attributeMappings.size(); m++ ) {
				final var attributeMapping = attributeMappings.get( m );
				final int jdbcTypeCount = attributeMapping.getJdbcTypeCount();
				for ( int i = 0; i < jdbcTypeCount; i++ ) {
					final var selectableMapping = attributeMapping.getSelectable( i );
					if ( selectableMapping.isPartitioned() ) {
						final String tableNameForMutation =
								persister.physicalTableNameForMutation( selectableMapping );
						final var tableMutationBuilder = resolver.apply( tableNameForMutation );
						if ( tableMutationBuilder != null ) {
							tableMutationBuilder.addKeyRestrictionLeniently( selectableMapping );
						}
					}
				}
			}
		}
	}

	MutationOperationGroup createMutationOperationGroup(TableUpdateBuilderStandard<MutationOperation> tableUpdateBuilder) {
		final var tableMutation = tableUpdateBuilder.buildMutation();
		return singleOperation(
				new MutationGroupSingle( MutationType.DELETE, entityPersister(), tableMutation ),
				tableMutation.createMutationOperation( null, factory() )
		);
	}
}
