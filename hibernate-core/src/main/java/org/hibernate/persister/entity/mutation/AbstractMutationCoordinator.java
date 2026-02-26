/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.Internal;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.internal.NoBatchKeyAccess;
import org.hibernate.engine.jdbc.mutation.spi.BatchKeyAccess;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.ast.MutationGroup;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.ast.builder.ColumnValuesTableMutationBuilder;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;
import org.hibernate.sql.model.ast.builder.RestrictedTableMutationBuilder;

import static java.lang.System.arraycopy;
import static org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper.identifiedResultsCheck;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.manyOperations;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.noOperations;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.singleOperation;


/**
 * Base support for coordinating mutations against an entity
 *
 * @implNote Split simply to help minimize the size of
 *           {@link org.hibernate.persister.entity.AbstractEntityPersister}
 *
 * @author Steve Ebersole
 */
@Internal
public abstract class AbstractMutationCoordinator {
	protected final EntityPersister entityPersister;
	protected final SessionFactoryImplementor factory;
	protected final MutationExecutorService mutationExecutorService;
	protected final Dialect dialect;

	public AbstractMutationCoordinator(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		this.entityPersister = entityPersister;
		this.factory = factory;
		dialect = factory.getJdbcServices().getDialect();
		mutationExecutorService = factory.getServiceRegistry().getService( MutationExecutorService.class );
	}

	static boolean hasValueGenerationOnExecution(
			OnExecutionGenerator generator,
			Dialect dialect,
			EventType eventType) {
		if ( generator.getEventTypes().contains( eventType ) ) {
			final boolean[] columnInclusions = generator.getColumnInclusions( dialect, eventType );
			if ( columnInclusions != null ) {
				for ( boolean included : columnInclusions ) {
					if ( !included ) {
						return true;
					}
				}
			}
			if ( !generator.referenceColumnsInSql( dialect, eventType ) ) {
				return false;
			}
			else if ( !generator.writePropertyValue( eventType ) ) {
				return true;
			}
			else {
				final String[] columnValues = generator.getReferencedColumnValues( dialect, eventType );
				if ( columnValues != null ) {
					for ( int i = 0; i < columnValues.length; i++ ) {
						if ( (columnInclusions == null || columnInclusions[i])
								&& !"?".equals( columnValues[i] ) ) {
							return true;
						}
					}
				}
				return false;
			}
		}
		else {
			return false;
		}
	}

	protected EntityPersister entityPersister() {
		return entityPersister;
	}

	protected SessionFactoryImplementor factory() {
		return factory;
	}

	protected Dialect dialect() {
		return dialect;
	}

	protected BatchKeyAccess resolveBatchKeyAccess(boolean dynamicUpdate, SharedSessionContractImplementor session) {
		if ( !dynamicUpdate && !entityPersister().optimisticLockStyle().isAllOrDirty() ) {
			final var transactionCoordinator = session.getTransactionCoordinator();
			if ( transactionCoordinator != null && transactionCoordinator.isTransactionActive() ) {
				return this::getBatchKey;
			}
		}

		return NoBatchKeyAccess.INSTANCE;
	}

	protected abstract BatchKey getBatchKey();

	protected MutationOperationGroup createOperationGroup(ValuesAnalysis valuesAnalysis, MutationGroup mutationGroup) {
		final int numberOfTableMutations = mutationGroup.getNumberOfTableMutations();
		switch ( numberOfTableMutations ) {
			case 0:
				return noOperations( mutationGroup );
			case 1: {
				final var operation = createOperation( valuesAnalysis, mutationGroup.getSingleTableMutation() );
				return operation == null
						? noOperations( mutationGroup )
						: singleOperation( mutationGroup, operation );
			}
			default: {
				var operations = new MutationOperation[numberOfTableMutations];
				int outputIndex = 0;
				int skipped = 0;
				for ( int i = 0; i < mutationGroup.getNumberOfTableMutations(); i++ ) {
					final var tableMutation = mutationGroup.getTableMutation( i );
					final var operation = tableMutation.createMutationOperation( valuesAnalysis, factory );
					if ( operation != null ) {
						operations[outputIndex++] = operation;
					}
					else {
						skipped++;
						MODEL_MUTATION_LOGGER.skippingUpdate( tableMutation.getTableName() );
					}
				}
				if ( skipped != 0 ) {
					final var trimmed = new MutationOperation[outputIndex];
					arraycopy( operations, 0, trimmed, 0, outputIndex );
					operations = trimmed;
				}
				return manyOperations( mutationGroup.getMutationType(), entityPersister, operations );
			}
		}
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected MutationOperation createOperation(ValuesAnalysis valuesAnalysis, TableMutation<?> singleTableMutation) {
		return singleTableMutation.createMutationOperation( valuesAnalysis, factory() );
	}

	boolean hasValueGenerationOnExecution(
			Object entity,
			SharedSessionContractImplementor session,
			OnExecutionGenerator generator,
			EventType eventType) {
		final boolean generatedOnExecution =
				session == null
						? generator.generatedOnExecution()
						: generator.generatedOnExecution( entity, session );
		return generatedOnExecution
			&& hasValueGenerationOnExecution( generator, dialect(), eventType );
	}

	protected void handleValueGeneration(
			AttributeMapping attributeMapping,
			MutationGroupBuilder mutationGroupBuilder,
			OnExecutionGenerator generator,
			EventType eventType) {
		final var dialect = dialect();
		final var columnValues = generator.getReferencedColumnValues( dialect, eventType );
		final var columnInclusions = generator.getColumnInclusions( dialect, eventType );
		attributeMapping.forEachSelectable( (j, mapping) -> {
			if ( columnInclusions == null || columnInclusions[j] ) {
				final ColumnValuesTableMutationBuilder<?> tableUpdateBuilder =
						mutationGroupBuilder.findTableDetailsBuilder(
								entityPersister.physicalTableNameForMutation( mapping ) );
				final String columnValue =
						columnValues != null && columnValues[j] != null
								? columnValues[j]
								: "?";
				tableUpdateBuilder.addValueColumn( columnValue, mapping );
			}
		} );
	}

	protected void bindPartitionColumnValueBindings(
			Object[] loadedState,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings) {
		final var persister = entityPersister();
		if ( persister.hasPartitionedSelectionMapping() ) {
			final var attributeMappings = persister.getAttributeMappings();
			final int size = attributeMappings.size();
			for ( int i = 0; i < size; i++ ) {
				final var attributeMapping = attributeMappings.get( i );
				if ( attributeMapping.hasPartitionedSelectionMapping() ) {
					attributeMapping.decompose(
							loadedState[i],
							0,
							jdbcValueBindings,
							null,
							(valueIndex, bindings, noop, value, jdbcValueMapping) -> {
								if ( jdbcValueMapping.isPartitioned() ) {
									bindings.bindValue(
											value,
											jdbcValueMapping,
											ParameterUsage.RESTRICT
									);
								}
							},
							session
					);
				}
			}
		}
	}

	protected static boolean needsRowId(EntityPersister entityPersister, EntityTableMapping tableMapping) {
		return entityPersister.getRowIdMapping() != null
			&& tableMapping.isIdentifierTable();
	}

	protected static void applyKeyRestriction(
			Object rowId,
			EntityPersister entityPersister,
			RestrictedTableMutationBuilder<?, ?> tableMutationBuilder,
			EntityTableMapping tableMapping) {
		if ( rowId != null && needsRowId( entityPersister, tableMapping ) ) {
			tableMutationBuilder.addKeyRestrictionLeniently( entityPersister.getRowIdMapping() );
		}
		else {
			tableMutationBuilder.addKeyRestrictions( tableMapping.getKeyMapping() );
		}
	}

	protected void breakDownKeyJdbcValues(
			Object id,
			Object rowId,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableMapping) {
		if ( rowId != null && needsRowId( entityPersister(), tableMapping ) ) {
			jdbcValueBindings.bindValue(
					rowId,
					tableMapping.getTableName(),
					entityPersister().getRowIdMapping().getRowIdName(),
					ParameterUsage.RESTRICT
			);
		}
		else {
			tableMapping.getKeyMapping().breakDownKeyJdbcValues(
					id,
					(jdbcValue, columnMapping) -> {
						jdbcValueBindings.bindValue(
								jdbcValue,
								tableMapping.getTableName(),
								columnMapping.getColumnName(),
								ParameterUsage.RESTRICT
						);
					},
					session
			);
		}
	}

	boolean resultCheck(
			Object id,
			PreparedStatementDetails statementDetails,
			int affectedRowCount,
			int batchPosition) {
		return identifiedResultsCheck(
				statementDetails,
				affectedRowCount,
				batchPosition,
				entityPersister(),
				id,
				factory()
		);
	}

	void applyOptimisticLocking(RestrictedTableMutationBuilder<?, ?> tableMutationBuilder) {
		if ( entityPersister().optimisticLockStyle() == OptimisticLockStyle.VERSION ) {
			applyVersionOptimisticLocking( tableMutationBuilder );
		}
	}

	void applyVersionOptimisticLocking(RestrictedTableMutationBuilder<?, ?> tableMutationBuilder) {
		final var versionMapping = entityPersister().getVersionMapping();
		if ( versionMapping != null ) {
			tableMutationBuilder.addOptimisticLockRestriction( versionMapping );
		}
	}

	StaleObjectStateException staleObjectStateException(Object id, StaleStateException cause) {
		return new StaleObjectStateException( entityPersister().getEntityName(), id, cause );
	}

	void applyPartitionKeyRestriction(RestrictedTableMutationBuilder<?, ?> tableMutationBuilder) {
		final var persister = entityPersister();
		if ( persister.hasPartitionedSelectionMapping() ) {
			final var attributeMappings = persister.getAttributeMappings();
			for ( int m = 0; m < attributeMappings.size(); m++ ) {
				final var attributeMapping = attributeMappings.get( m );
				final int jdbcTypeCount = attributeMapping.getJdbcTypeCount();
				for ( int i = 0; i < jdbcTypeCount; i++ ) {
					final var selectableMapping = attributeMapping.getSelectable( i );
					if ( selectableMapping.isPartitioned() ) {
						tableMutationBuilder.addKeyRestrictionLeniently( selectableMapping );
					}
				}
			}
		}
	}

	/**
	 * For temporal history tables and audit log tables.
	 */
	static EntityTableMapping createAuxiliaryTableMapping(
			EntityTableMapping identifierTableMapping,
			EntityPersister persister,
			String tableName) {
		return new EntityTableMapping(
				tableName,
				identifierTableMapping.getRelativePosition(),
				identifierTableMapping.getKeyMapping(),
				identifierTableMapping.isOptional(),
				identifierTableMapping.isInverse(),
				identifierTableMapping.isIdentifierTable(),
				identifierTableMapping.getAttributeIndexes(),
				identifierTableMapping.getInsertExpectation(),
				identifierTableMapping.getInsertCustomSql(),
				identifierTableMapping.isInsertCallable(),
				identifierTableMapping.getUpdateExpectation(),
				identifierTableMapping.getUpdateCustomSql(),
				identifierTableMapping.isUpdateCallable(),
				identifierTableMapping.isCascadeDeleteEnabled(),
				identifierTableMapping.getDeleteExpectation(),
				identifierTableMapping.getDeleteCustomSql(),
				identifierTableMapping.isDeleteCallable(),
				persister.isDynamicUpdate(),
				persister.isDynamicInsert()
		);
	}}
