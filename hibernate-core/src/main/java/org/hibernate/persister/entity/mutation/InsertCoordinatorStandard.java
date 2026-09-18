/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.batch.internal.EntityInsertBatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.spi.mutation.ValuesAnalysis;
import org.hibernate.sql.ast.spi.model.builder.MutationGroupBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableInsertBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableInsertBuilderStandard;
import org.hibernate.sql.ast.spi.model.builder.TableMutationBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import static org.hibernate.generator.EventType.INSERT;

/**
 * Coordinates the insertion of an entity.
 *
 * @see #coordinateInsert
 *
 * @author Steve Ebersole
 */
@Internal
public class InsertCoordinatorStandard extends AbstractMutationCoordinator implements InsertCoordinator {
	@Nullable
	private final MutationOperationGroup staticInsertGroup;
	@Nullable
	private final BatchKey batchKey;

	public InsertCoordinatorStandard(@Nonnull EntityPersister entityPersister, @Nonnull SessionFactoryImplementor factory) {
		super( entityPersister, factory );

		batchKey =
				entityPersister.isIdentifierAssignedByInsert() || entityPersister.hasInsertGeneratedProperties()
						// disable batching in case of insert-generated identifier or properties
						? null
						: new EntityInsertBatchKey( entityPersister.getEntityName() + "#INSERT" );

		staticInsertGroup =
				entityPersister.isDynamicInsert()
						// the entity specified dynamic-insert - skip generating the
						// static inserts as we will create them every time
						? null
						: generateStaticOperationGroup();
	}

	@Nullable
	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return staticInsertGroup;
	}

	@Nullable
	@Override
	protected BatchKey getBatchKey() {
		return batchKey;
	}

	@Override
	public @Nullable GeneratedValues insert(@Nonnull Object entity, @Nonnull Object[] values, @Nonnull SharedSessionContractImplementor session) {
		return coordinateInsert( null, values, entity, session );
	}

	@Override
	public @Nullable GeneratedValues insert(
			@Nonnull Object entity,
			@Nullable Object id,
			@Nonnull Object[] values,
			@Nonnull SharedSessionContractImplementor session) {
		return coordinateInsert( id, values, entity, session );
	}

	/**
	 * Perform the insert(s).
	 *
	 * @param id This is the id as known in memory. For post-insert id generation (IDENTITY, etc)
	 * this will be null.
	 * @param values The extracted attribute values
	 * @param entity The entity instance being persisted
	 * @param session The originating context
	 *
	 * @return The {@linkplain GeneratedValues generated values} if any, {@code null} otherwise.
	 */
	@Nullable
	public GeneratedValues coordinateInsert(
			@Nullable Object id,
			@Nonnull Object[] values,
			@Nonnull Object entity,
			@Nonnull SharedSessionContractImplementor session) {
		// apply any pre-insert in-memory value generation
		final boolean needsDynamicInsert = preInsertInMemoryValueGeneration( values, entity, session );
		final var persister = entityPersister();
		final boolean forceIdentifierBinding = persister.getGenerator().generatedOnExecution() && id != null;
		return persister.isDynamicInsert()
			|| needsDynamicInsert
			|| forceIdentifierBinding
				? doDynamicInserts( id, values, entity, session, forceIdentifierBinding )
				: doStaticInserts( id, values, entity, session );
	}

	protected boolean preInsertInMemoryValueGeneration(@Nonnull Object[] values, @Nonnull Object entity, @Nonnull SharedSessionContractImplementor session) {
		final var persister = entityPersister();
		boolean foundStateDependentGenerator = false;
		if ( persister.hasPreInsertGeneratedProperties() ) {
			final var generators = persister.getGenerators();
			for ( int i = 0; i < generators.length; i++ ) {
				final var generator = generators[i];
				if ( generator != null
						&& generator.generatesOnInsert()
						&& generator.generatedBeforeExecution( entity, session ) ) {
					values[i] = ( (BeforeExecutionGenerator) generator ).generate( session, entity, values[i], INSERT );
					persister.setValue( entity, i, values[i] );
					foundStateDependentGenerator = foundStateDependentGenerator || generator.generatedOnExecution();
				}
			}
		}
		return foundStateDependentGenerator;
	}

	public static class InsertValuesAnalysis implements ValuesAnalysis {
		private final List<TableMapping> tablesWithNonNullValues = new ArrayList<>();

		public InsertValuesAnalysis(@Nonnull EntityMutationTarget mutationTarget, @Nonnull Object[] values) {
			mutationTarget.forEachMutableTable( (tableMapping) -> {
				for ( int tableAttributeIndex : tableMapping.getAttributeIndexes() ) {
					if ( values[tableAttributeIndex] != null ) {
						tablesWithNonNullValues.add( tableMapping );
						break;
					}
				}
			} );
		}

		public boolean hasNonNullBindings(@Nonnull TableMapping tableMapping) {
			return tablesWithNonNullValues.contains( tableMapping );
		}
	}

	@Nullable
	protected GeneratedValues doStaticInserts(@Nullable Object id, @Nonnull Object[] values, @Nonnull Object object, @Nonnull SharedSessionContractImplementor session) {
		final var insertValuesAnalysis = new InsertValuesAnalysis( entityPersister(), values );

		final var tableInclusionChecker = getTableInclusionChecker( insertValuesAnalysis );

		final var mutationExecutor = executor( session, castNonNull( staticInsertGroup ), false );

		decomposeForInsert(
				mutationExecutor,
				id,
				values,
				object,
				castNonNull( staticInsertGroup ),
				entityPersister().getPropertyInsertability(),
				tableInclusionChecker,
				session
		);

		try {
			return mutationExecutor.execute(
					object,
					insertValuesAnalysis,
					tableInclusionChecker,
					InsertCoordinatorStandard::verifyOutcome,
					session
			);
		}
		finally {
			mutationExecutor.release();
		}
	}

	protected void 	decomposeForInsert(
			@Nonnull MutationExecutor mutationExecutor,
			@Nullable Object id,
			@Nonnull Object[] values,
			@Nonnull Object object,
			@Nonnull MutationOperationGroup mutationGroup,
			@Nonnull boolean[] propertyInclusions,
			@Nonnull TableInclusionChecker tableInclusionChecker,
			@Nonnull SharedSessionContractImplementor session) {
		final var jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
		final var attributeMappings = entityPersister().getAttributeMappings();

		for ( int position = 0; position < mutationGroup.getNumberOfOperations(); position++ ) {
			final var operation = mutationGroup.getOperation( position );
			final var tableDetails = (EntityTableMapping) operation.getTableDetails();
			if ( tableInclusionChecker.include( tableDetails ) ) {
				for ( final int attributeIndex : tableDetails.getAttributeIndexes() ) {
					if ( propertyInclusions[attributeIndex] ) {
						final var attributeMapping = attributeMappings.get( attributeIndex );
						decomposeAttribute(
								values[attributeIndex],
								session,
								jdbcValueBindings,
								attributeMapping,
								attributeMapping.getGenerator(),
								object
						);
					}
				}
			}
		}

		if ( id == null ) {
			assert entityPersister().getInsertDelegate() != null;
			bindGeneratedIdentifierJdbcValues( object, session, jdbcValueBindings, mutationGroup );
		}
		else {
			for ( int position = 0; position < mutationGroup.getNumberOfOperations(); position++ ) {
				final var jdbcOperation = mutationGroup.getOperation( position );
				final var tableDetails = (EntityTableMapping) jdbcOperation.getTableDetails();
				breakDownJdbcValue( id, session, jdbcValueBindings, tableDetails );
			}
		}
	}

	private void bindGeneratedIdentifierJdbcValues(
			@Nonnull Object entity,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings,
			@Nonnull MutationOperationGroup mutationGroup) {
		if ( entityPersister().getGenerator()
					instanceof CompositeNestedGeneratedValueGenerator compositeGenerator ) {
			final boolean[] columnInclusions =
					compositeGenerator.getColumnInclusions( dialect(), EventType.INSERT );
			final String[] columnValues =
					compositeGenerator.getReferencedColumnValues( dialect(), EventType.INSERT );
			final boolean bindAllIncluded =
					columnValues == null && compositeGenerator.writePropertyValue( EventType.INSERT );
			if ( bindAllIncluded || hasParameterMarkers( columnValues, columnInclusions ) ) {
				final Object idToBind = entityPersister().getIdentifier( entity, session );
				if ( idToBind != null ) {
					for ( int position = 0; position < mutationGroup.getNumberOfOperations(); position++ ) {
						breakDownJdbcValue(
								idToBind,
								session,
								jdbcValueBindings,
								(EntityTableMappingImpl)
										mutationGroup.getOperation( position )
												.getTableDetails(),
								columnInclusions,
								columnValues,
								bindAllIncluded
						);
					}
				}
			}
		}
	}

	private static boolean hasParameterMarkers(@Nullable String[] columnValues, @Nullable boolean[] columnInclusions) {
		if ( columnValues != null ) {
			for ( int i = 0; i < columnValues.length; i++ ) {
				if ( (columnInclusions == null || i >= columnInclusions.length || columnInclusions[i])
					&& "?".equals( columnValues[i] ) ) {
					return true;
				}
			}
		}
		return false;
	}

	protected void breakDownJdbcValue(
			@Nonnull Object id,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings,
			@Nonnull EntityTableMapping tableDetails) {
		final String tableName = tableDetails.getTableName();
		tableDetails.getKeyMapping().breakDownKeyJdbcValues(
				id,
				(jdbcValue, columnMapping) -> {
					jdbcValueBindings.bindValue(
							jdbcValue,
							tableName,
							columnMapping.getColumnName(),
							ParameterUsage.SET
					);
				},
				session
		);
	}

	protected void breakDownJdbcValue(
			@Nonnull Object id,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings,
			@Nonnull EntityTableMappingImpl tableDetails,
			@Nonnull boolean[] columnInclusions,
			@Nonnull String[] columnValues,
			boolean bindAllIncluded) {
		final String tableName = tableDetails.getTableName();
		final var keyMapping = tableDetails.getKeyMapping();
		final var keyColumns = keyMapping.getKeyColumns();
		final var keyColumnIndex =
				new IdentityHashMap<TableDetails.KeyColumn, Integer>( keyColumns.size() );
		for ( int i = 0; i < keyColumns.size(); i++ ) {
			keyColumnIndex.put( keyColumns.get( i ), i );
		}
		keyMapping.breakDownKeyJdbcValues(
				id,
				(jdbcValue, columnMapping) -> {
					final Integer index = keyColumnIndex.get( columnMapping );
					if ( index != null
							&& shouldBindKeyColumn( index, columnInclusions, columnValues, bindAllIncluded ) ) {
						jdbcValueBindings.bindValue(
								jdbcValue,
								tableName,
								columnMapping.getColumnName(),
								ParameterUsage.SET
						);
					}
				},
				session
		);
	}

	private static boolean shouldBindKeyColumn(
			int index,
			@Nullable boolean[] columnInclusions,
			@Nullable String[] columnValues,
			boolean bindAllIncluded) {
		if ( columnInclusions != null
				&& ( index >= columnInclusions.length || !columnInclusions[index] ) ) {
			return false;
		}
		else if ( columnValues == null ) {
			return bindAllIncluded;
		}
		else {
			return index < columnValues.length
				&& "?".equals( columnValues[index] );
		}
	}

	protected void decomposeAttribute(
			@Nonnull Object value,
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull JdbcValueBindings jdbcValueBindings,
			@Nonnull AttributeMapping mapping,
			@Nonnull Generator generator,
			@Nonnull Object entity) {
		if ( !(mapping instanceof PluralAttributeMapping) ) {
			final OnExecutionGenerator onExecutionGenerator;
			final String[] columnValues;
			final boolean[] columnInclusions;
			final boolean bindAllValues;
			if ( generator instanceof OnExecutionGenerator executionGenerator
					&& generator.generatedOnExecution( entity, session )
					&& generator.generatesOnInsert() ) {
				onExecutionGenerator = executionGenerator;
				columnValues = onExecutionGenerator.getReferencedColumnValues( dialect(), EventType.INSERT );
				columnInclusions = onExecutionGenerator.getColumnInclusions( dialect(), EventType.INSERT );
				bindAllValues = onExecutionGenerator.writePropertyValue( EventType.INSERT ) && columnValues == null;
			}
			else {
				onExecutionGenerator = null;
				columnValues = null;
				columnInclusions = null;
				bindAllValues = false;
			}

			mapping.decompose(
					value,
					0,
					jdbcValueBindings,
					null,
					(valueIndex, bindings, noop, jdbcValue, selectableMapping) -> {
						if ( selectableMapping.isInsertable()
								&& shouldBindValue( onExecutionGenerator, columnValues, columnInclusions, bindAllValues, valueIndex ) ) {
							bindings.bindValue(
									jdbcValue,
									entityPersister().physicalTableNameForMutation( selectableMapping ),
									selectableMapping.getSelectionExpression(),
									ParameterUsage.SET
							);
						}
					},
					session
			);
		}
	}

	private static boolean shouldBindValue(
			@Nullable OnExecutionGenerator onExecutionGenerator,
			@Nullable String[] columnValues,
			@Nullable boolean[] columnInclusions,
			boolean bindAllValues,
			int valueIndex) {
		if ( onExecutionGenerator == null ) {
			return true;
		}
		else if ( columnInclusions != null && !columnInclusions[valueIndex] ) {
			return false;
		}
		else {
			return bindAllValues
				|| columnValues != null && "?".equals( columnValues[valueIndex] );
		}
	}

	@Nullable
	protected GeneratedValues doDynamicInserts(
			@Nullable Object id,
			@Nonnull Object[] values,
			@Nonnull Object object,
			@Nonnull SharedSessionContractImplementor session,
			boolean forceIdentifierBinding) {
		final boolean[] propertiesToInsert = getPropertiesToInsert( entityPersister(), values );
		final var insertGroup =
				generateDynamicInsertSqlGroup( propertiesToInsert, object, session, forceIdentifierBinding );
		final var mutationExecutor = executor( session, insertGroup, true );
		final var insertValuesAnalysis = new InsertValuesAnalysis( entityPersister(), values );
		final var tableInclusionChecker = getTableInclusionChecker( insertValuesAnalysis );
		decomposeForInsert( mutationExecutor, id, values, object, insertGroup, propertiesToInsert, tableInclusionChecker, session );
		try {
			return mutationExecutor.execute(
					object,
					insertValuesAnalysis,
					tableInclusionChecker,
					InsertCoordinatorStandard::verifyOutcome,
					session
			);
		}
		finally {
			mutationExecutor.release();
		}
	}

	private static boolean verifyOutcome(@Nonnull PreparedStatementDetails statementDetails, int affectedRowCount, int batchPosition)
			throws SQLException {
		statementDetails.getExpectation().verifyOutcome(
				affectedRowCount,
				statementDetails.getStatement(),
				batchPosition,
				statementDetails.getSqlString()
		);
		return true;
	}

	@Nonnull
	private MutationExecutor executor(@Nonnull SharedSessionContractImplementor session, @Nonnull MutationOperationGroup group, boolean dynamicUpdate) {
		return mutationExecutorService
				.createExecutor( resolveBatchKeyAccess( dynamicUpdate, session ), group, session );
	}

	@Nonnull
	protected static TableInclusionChecker getTableInclusionChecker(@Nonnull InsertValuesAnalysis insertValuesAnalysis) {
		return tableMapping -> !tableMapping.isOptional()
			|| insertValuesAnalysis.hasNonNullBindings( tableMapping );
	}


	/**
	 * Transform the array of property indexes to an array of booleans,
	 * true when the property is insertable and non-null
	 */
	@Nonnull
	static boolean[] getPropertiesToInsert(@Nonnull EntityPersister persister, @Nonnull Object[] fields) {
		final var notNull = new boolean[fields.length];
		final var insertable = persister.getPropertyInsertability();
		for ( int i = 0; i < fields.length; i++ ) {
			notNull[i] = insertable[i] && fields[i] != null;
		}
		return notNull;
	}

	@Nonnull
	protected MutationOperationGroup generateDynamicInsertSqlGroup(
			@Nonnull boolean[] insertable,
			@Nonnull Object object,
			@Nonnull SharedSessionContractImplementor session,
			boolean forceIdentifierBinding) {
		final var insertGroupBuilder = new MutationGroupBuilder( MutationType.INSERT, entityPersister() );
		entityPersister().forEachMutableTable(
				(tableMapping) -> insertGroupBuilder.addTableDetailsBuilder( createTableInsertBuilder( tableMapping, forceIdentifierBinding ) )
		);
		applyTableInsertDetails( insertGroupBuilder, insertable, object, session, forceIdentifierBinding );
		return createOperationGroup( null, insertGroupBuilder.buildMutationGroup() );
	}

	@Nonnull
	public MutationOperationGroup generateStaticOperationGroup() {
		final var persister = entityPersister();
		final var insertGroupBuilder = new MutationGroupBuilder( MutationType.INSERT, persister );
		persister.forEachMutableTable(
				(tableMapping) -> insertGroupBuilder.addTableDetailsBuilder( createTableInsertBuilder( tableMapping, false ) )
		);
		applyTableInsertDetails( insertGroupBuilder, persister.getPropertyInsertability(), null, null, false );
		return createOperationGroup( null, insertGroupBuilder.buildMutationGroup() );
	}

	@Nonnull
	private TableMutationBuilder<?> createTableInsertBuilder(
			@Nonnull EntityTableMapping tableMapping,
			boolean forceIdentifierBinding) {
		final var persister = entityPersister();
		final var delegate = persister.getInsertDelegate();
		return tableMapping.isIdentifierTable()
			&& delegate != null
			&& !forceIdentifierBinding
				? delegate.createTableMutationBuilder( tableMapping.getInsertExpectation(), factory() )
				: new TableInsertBuilderStandard( persister, tableMapping, factory() );
	}

	private void applyTableInsertDetails(
			@Nonnull MutationGroupBuilder insertGroupBuilder,
			@Nonnull boolean[] attributeInclusions,
			@Nullable Object object,
			@Nullable SharedSessionContractImplementor session,
			boolean forceIdentifierBinding) {
		final var attributeMappings = entityPersister().getAttributeMappings();

		insertGroupBuilder.forEachTableMutationBuilder( (builder) -> {
			final var tableMapping = (EntityTableMapping) builder.getMutatingTable().getTableMapping();
			assert !tableMapping.isInverse();

			// `attributeIndexes` represents the indexes (relative to `attributeMappings`) of
			// the attributes mapped to the table
			for ( final int attributeIndex : tableMapping.getAttributeIndexes() ) {
				final var attributeMapping = attributeMappings.get( attributeIndex );
				final var generator = attributeMapping.getGenerator();
				if ( generator instanceof OnExecutionGenerator onExecutionGenerator
						&& hasValueGenerationOnExecution( object, session, onExecutionGenerator, EventType.INSERT ) ) {
					if ( needsValueBinding( onExecutionGenerator, dialect() ) ) {
						attributeInclusions[attributeIndex] = true;
					}
					handleValueGeneration( attributeMapping, insertGroupBuilder, onExecutionGenerator, EventType.INSERT );
				}
				else if ( attributeInclusions[attributeIndex] ) {
					attributeMapping.forEachInsertable( insertGroupBuilder );
				}
				else if ( generator != null && generator.generatesOnInsert() ) {
					if ( session != null && generator.generatedBeforeExecution( object, session ) ) {
						attributeInclusions[attributeIndex] = true;
						attributeMapping.forEachInsertable( insertGroupBuilder );
					}
				}
			}
		} );

		// add the discriminator
		entityPersister().addDiscriminatorToInsertGroup( insertGroupBuilder );
		entityPersister().addAuxiliaryToInsertGroup( insertGroupBuilder );

		// add the keys
		insertGroupBuilder.forEachTableMutationBuilder( (tableMutationBuilder) -> {
			final var tableInsertBuilder = (TableInsertBuilder) tableMutationBuilder;
			final var tableMapping = (EntityTableMapping) tableInsertBuilder.getMutatingTable().getTableMapping();
			final var keyMapping = tableMapping.getKeyMapping();
			if ( tableMapping.isIdentifierTable()
					&& entityPersister().isIdentifierAssignedByInsert()
					&& !forceIdentifierBinding ) {
				assert entityPersister().getInsertDelegate() != null;
				final var generator = (OnExecutionGenerator) entityPersister().getGenerator();
				final boolean[] columnInclusions = generator.getColumnInclusions( dialect, EventType.INSERT );
				final String[] columnValues = generator.getReferencedColumnValues( dialect, EventType.INSERT );
				final int keyColumnCount = keyMapping.getColumnCount();
				if ( columnInclusions != null ) {
					if ( columnValues != null && columnValues.length != keyColumnCount ) {
						throw new IllegalStateException(
								"Mismatch between generated column values and identifier columns for "
										+ entityPersister().getEntityName()
						);
					}
					for ( int i = 0; i < keyColumnCount; i++ ) {
						if ( columnInclusions[i] ) {
							final String valueExpression =
									columnValues == null
											? keyMapping.getKeyColumn( i ).getWriteExpression()
											: columnValues[i];
							tableInsertBuilder.addColumnAssignment( keyMapping.getKeyColumn( i ), valueExpression );
						}
					}
				}
				else if ( generator.referenceColumnsInSql( dialect, EventType.INSERT ) ) {
					if ( columnValues != null ) {
						assert columnValues.length == 1;
						assert keyColumnCount == 1;
						tableInsertBuilder.addValueColumn( columnValues[0], keyMapping.getKeyColumn( 0 ) );
					}
				}
			}
			else {
				keyMapping.forEachKeyColumn( tableInsertBuilder::addValueColumn );
			}
		} );
	}

	private static boolean needsValueBinding(@Nonnull OnExecutionGenerator generator, @Nonnull Dialect dialect) {
		if ( generator.generatesOnInsert() ) {
			final boolean[] columnInclusions = generator.getColumnInclusions( dialect, EventType.INSERT );
			final String[] columnValues = generator.getReferencedColumnValues( dialect, EventType.INSERT );
			if ( columnValues != null ) {
				for ( int i = 0; i < columnValues.length; i++ ) {
					if ( (columnInclusions == null || columnInclusions[i])
							&& "?".equals( columnValues[i] ) ) {
						return true;
					}
				}
				return false;
			}
			else {
				return generator.writePropertyValue( EventType.INSERT );
			}
		}
		else {
			return false;
		}
	}

	/**
	 * @deprecated Use {@link #getBatchKey()}
	 */
	@Nullable
	@Deprecated
	public BatchKey getInsertBatchKey() {
		return batchKey;
	}
}
