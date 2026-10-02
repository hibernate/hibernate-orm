package org.hibernate.persister.entity.mutation;

import org.hibernate.generator.values.GeneratedValues;

import org.hibernate.AssertionFailure;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.internal.TenantIdHelper;
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
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.ValuesAnalysis;
import org.hibernate.sql.ast.spi.model.MutationGroup;
import org.hibernate.sql.ast.spi.model.TableMutation;
import org.hibernate.sql.ast.spi.model.builder.AssigningTableMutationBuilder;
import org.hibernate.sql.ast.spi.model.builder.MutationGroupBuilder;
import org.hibernate.sql.ast.spi.model.builder.RestrictedTableMutationBuilder;

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

	public AbstractMutationCoordinator(@Nonnull EntityPersister entityPersister, @Nonnull SessionFactoryImplementor factory) {
		this.entityPersister = entityPersister;
		this.factory = factory;
		dialect = factory.getJdbcServices().getDialect();
		mutationExecutorService = factory.getServiceRegistry().getService( MutationExecutorService.class );
	}

	@Nonnull
	protected Object resolveInsertedIdentifier(
			@Nonnull Object entity,
			@Nullable Object id,
			@Nullable GeneratedValues generatedValues,
			@Nonnull SharedSessionContractImplementor session) {
		if ( id != null ) {
			return id;
		}
		final Object generatedId = generatedValues == null
				? null
				: generatedValues.getGeneratedValue( entityPersister.getIdentifierMapping() );
		if ( generatedId != null ) {
			return generatedId;
		}
		final Object entityId = entityPersister.getIdentifier( entity, session );
		if ( entityId == null ) {
			throw new AssertionFailure( "No identifier after insert of '"
					+ entityPersister.getEntityName() + "'" );
		}
		return entityId;
	}

	static boolean hasValueGenerationOnExecution(
			@Nonnull OnExecutionGenerator generator,
			@Nonnull Dialect dialect,
			@Nonnull EventType eventType) {
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

	protected void applyTenantRestriction(@Nonnull RestrictedTableMutationBuilder<?, ?> builder) {
		TenantIdHelper.applyTenantRestriction( entityPersister(), builder );
	}

	protected void bindTenantRestriction(
			@Nonnull SharedSessionContractImplementor session, @Nonnull JdbcValueBindings bindings, @Nonnull MutationOperationGroup operationGroup) {
		final var tenantMapping = TenantIdHelper.tenantIdAttribute( entityPersister() );
		if ( tenantMapping != null ) {
			final var selectable = tenantMapping.getSelectable( 0 );
			final String tableName = entityPersister().physicalTableNameForMutation( selectable );
			final var operation = operationGroup.getOperation( tableName );
			if ( operation != null
					&& TenantIdHelper.tenantIdColumn( entityPersister(), operation ) != null ) {
				bindings.bindValue(
						session.isRootTenant() ? null : session.getTenantIdentifierValue(),
						tableName, selectable.getSelectionExpression(), ParameterUsage.TENANT );
			}
		}
	}

	@Nonnull
	protected EntityPersister entityPersister() {
		return entityPersister;
	}

	@Nonnull
	protected SessionFactoryImplementor factory() {
		return factory;
	}

	@Nonnull
	protected Dialect dialect() {
		return dialect;
	}

	@Nonnull
	protected BatchKeyAccess resolveBatchKeyAccess(boolean dynamicUpdate, @Nonnull SharedSessionContractImplementor session) {
		if ( !dynamicUpdate && !entityPersister().optimisticLockStyle().isAllOrDirty() ) {
			final var transactionCoordinator = session.getTransactionCoordinator();
			if ( transactionCoordinator != null && transactionCoordinator.isTransactionActive() ) {
				return this::getBatchKey;
			}
		}

		return NoBatchKeyAccess.INSTANCE;
	}

	@Nullable
	protected abstract BatchKey getBatchKey();

	@Nonnull
	protected MutationOperationGroup createOperationGroup(@Nullable ValuesAnalysis valuesAnalysis, @Nonnull MutationGroup mutationGroup) {
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
	@Nonnull
	protected MutationOperation createOperation(@Nullable ValuesAnalysis valuesAnalysis, @Nonnull TableMutation<?> singleTableMutation) {
		return singleTableMutation.createMutationOperation( valuesAnalysis, factory() );
	}

	// Used by Hibernate Reactive
	protected boolean hasValueGenerationOnExecution(
			@Nullable Object entity,
			@Nullable SharedSessionContractImplementor session,
			@Nonnull OnExecutionGenerator generator,
			@Nonnull EventType eventType) {
		final boolean generatedOnExecution =
				session == null
						? generator.generatedOnExecution()
						: generator.generatedOnExecution( entity, session );
		return generatedOnExecution
			&& hasValueGenerationOnExecution( generator, dialect(), eventType );
	}

	protected void handleValueGeneration(
			@Nonnull AttributeMapping attributeMapping,
			@Nonnull MutationGroupBuilder mutationGroupBuilder,
			@Nonnull OnExecutionGenerator generator,
			@Nonnull EventType eventType) {
		final var dialect = dialect();
		final var columnValues = generator.getReferencedColumnValues( dialect, eventType );
		final var columnInclusions = generator.getColumnInclusions( dialect, eventType );
		attributeMapping.forEachSelectable( (j, mapping) -> {
			if ( columnInclusions == null || columnInclusions[j] ) {
				final AssigningTableMutationBuilder<?> tableUpdateBuilder =
						mutationGroupBuilder.findTableDetailsBuilder(
								entityPersister.physicalTableNameForMutation( mapping ) );
				final String columnValue =
						columnValues != null && columnValues[j] != null
								? columnValues[j]
								: "?";
				tableUpdateBuilder.addColumnAssignment( mapping, columnValue );
			}
		} );
	}

	protected void bindPartitionColumnValueBindings(
			@Nullable Object[] loadedState,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings) {
		final var persister = entityPersister();
		if ( persister.hasPartitionedSelectionMapping() ) {
			if ( loadedState == null ) {
				throw new AssertionFailure( "No loaded state for partitioned entity '"
						+ persister.getEntityName() + "'" );
			}
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

	protected static boolean needsRowId(@Nonnull EntityPersister entityPersister, @Nonnull EntityTableMapping tableMapping) {
		return entityPersister.getRowIdMapping() != null
			&& tableMapping.isIdentifierTable();
	}

	protected static void applyKeyRestriction(
			@Nullable Object rowId,
			@Nonnull EntityPersister entityPersister,
			@Nonnull RestrictedTableMutationBuilder<?, ?> tableMutationBuilder,
			@Nonnull EntityTableMapping tableMapping) {
		if ( rowId != null && needsRowId( entityPersister, tableMapping ) ) {
			tableMutationBuilder.addKeyRestrictionLeniently( entityPersister.getRowIdMapping() );
		}
		else {
			tableMutationBuilder.addKeyRestrictions( tableMapping.getKeyMapping() );
		}
	}

	protected void breakDownKeyJdbcValues(
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings,
			@Nonnull EntityTableMapping tableMapping) {
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
			@Nonnull Object id,
			@Nonnull PreparedStatementDetails statementDetails,
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

	void applyOptimisticLocking(@Nonnull RestrictedTableMutationBuilder<?, ?> tableMutationBuilder) {
		if ( entityPersister().optimisticLockStyle() == OptimisticLockStyle.VERSION ) {
			applyVersionOptimisticLocking( tableMutationBuilder );
		}
	}

	void applyVersionOptimisticLocking(@Nonnull RestrictedTableMutationBuilder<?, ?> tableMutationBuilder) {
		final var versionMapping = entityPersister().getVersionMapping();
		if ( versionMapping != null ) {
			tableMutationBuilder.addOptimisticLockRestriction( versionMapping );
		}
	}

	@Nonnull
	StaleObjectStateException staleObjectStateException(@Nonnull Object id, @Nonnull StaleStateException cause) {
		return new StaleObjectStateException( entityPersister().getEntityName(), id, cause );
	}

	void applyPartitionKeyRestriction(@Nonnull RestrictedTableMutationBuilder<?, ?> tableMutationBuilder) {
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
	@Nonnull
	public static EntityTableMapping createAuxiliaryTableMapping(
			@Nonnull EntityTableMapping identifierTableMapping,
			@Nonnull EntityPersister persister,
			@Nonnull String tableName) {
		return new EntityTableMappingImpl(
				tableName,
				identifierTableMapping.relativePosition(),
				identifierTableMapping.getKeyMapping(),
				identifierTableMapping.isOptional(),
				identifierTableMapping.isInverse(),
				identifierTableMapping.isIdentifierTable(),
				false,
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
	}
}
