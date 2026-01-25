/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
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
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableMutationBuilder;

import org.checkerframework.checker.nullness.qual.Nullable;

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
	private final MutationOperationGroup staticInsertGroup;
	private final BasicBatchKey batchKey;

	public InsertCoordinatorStandard(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );

		batchKey =
				entityPersister.isIdentifierAssignedByInsert() || entityPersister.hasInsertGeneratedProperties()
						// disable batching in case of insert-generated identifier or properties
						? null
						: new BasicBatchKey( entityPersister.getEntityName() + "#INSERT" );

		staticInsertGroup =
				entityPersister.isDynamicInsert()
						// the entity specified dynamic-insert - skip generating the
						// static inserts as we will create them every time
						? null
						: generateStaticOperationGroup();
	}

	@Override
	public MutationOperationGroup getStaticMutationOperationGroup() {
		return staticInsertGroup;
	}

	@Override
	protected BatchKey getBatchKey() {
		return batchKey;
	}

	@Override
	public @Nullable GeneratedValues insert(Object entity, Object[] values, SharedSessionContractImplementor session) {
		return coordinateInsert( null, values, entity, session );
	}

	@Override
	public @Nullable GeneratedValues insert(
			Object entity,
			Object id,
			Object[] values,
			SharedSessionContractImplementor session) {
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
	public GeneratedValues coordinateInsert(
			Object id,
			Object[] values,
			Object entity,
			SharedSessionContractImplementor session) {
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

	protected boolean preInsertInMemoryValueGeneration(Object[] values, Object entity, SharedSessionContractImplementor session) {
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

		public InsertValuesAnalysis(EntityMutationTarget mutationTarget, Object[] values) {
			mutationTarget.forEachMutableTable( (tableMapping) -> {
				final int[] tableAttributeIndexes = tableMapping.getAttributeIndexes();
				for ( int i = 0; i < tableAttributeIndexes.length; i++ ) {
					if ( values[tableAttributeIndexes[i]] != null ) {
						tablesWithNonNullValues.add( tableMapping );
						break;
					}
				}
			} );
		}

		public boolean hasNonNullBindings(TableMapping tableMapping) {
			return tablesWithNonNullValues.contains( tableMapping );
		}
	}

	protected GeneratedValues doStaticInserts(Object id, Object[] values, Object object, SharedSessionContractImplementor session) {
		final var insertValuesAnalysis = new InsertValuesAnalysis( entityPersister(), values );

		final var tableInclusionChecker = getTableInclusionChecker( insertValuesAnalysis );

		final var mutationExecutor = executor( session, staticInsertGroup, false );

		decomposeForInsert(
				mutationExecutor,
				id,
				values,
				object,
				staticInsertGroup,
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

	protected void decomposeForInsert(
			MutationExecutor mutationExecutor,
			Object id,
			Object[] values,
			Object object,
			MutationOperationGroup mutationGroup,
			boolean[] propertyInclusions,
			TableInclusionChecker tableInclusionChecker,
			SharedSessionContractImplementor session) {
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

		final var temporalMapping = entityPersister().getTemporalMapping();
		if ( temporalMapping != null ) {
			jdbcValueBindings.bindValue(
					session.getTransactionStartInstant(),
					entityPersister().physicalTableNameForMutation( temporalMapping.getStartingColumnMapping() ),
					temporalMapping.getStartingColumnMapping().getSelectionExpression(),
					ParameterUsage.SET
			);
		}
	}

	private void bindGeneratedIdentifierJdbcValues(
			Object entity,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			MutationOperationGroup mutationGroup) {
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
								(EntityTableMapping)
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

	private static boolean hasParameterMarkers(String[] columnValues, boolean[] columnInclusions) {
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
			Object id,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableDetails) {
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
			Object id,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			EntityTableMapping tableDetails,
			boolean[] columnInclusions,
			String[] columnValues,
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
			boolean[] columnInclusions,
			String[] columnValues,
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
			Object value,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			AttributeMapping mapping,
			Generator generator,
			Object entity) {
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
			OnExecutionGenerator onExecutionGenerator,
			String[] columnValues,
			boolean[] columnInclusions,
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

	protected GeneratedValues doDynamicInserts(
			Object id,
			Object[] values,
			Object object,
			SharedSessionContractImplementor session,
			boolean forceIdentifierBinding) {
		final boolean[] propertiesToInsert = getPropertiesToInsert( values );
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

	private static boolean verifyOutcome(PreparedStatementDetails statementDetails, int affectedRowCount, int batchPosition)
			throws SQLException {
		statementDetails.getExpectation().verifyOutcome(
				affectedRowCount,
				statementDetails.getStatement(),
				batchPosition,
				statementDetails.getSqlString()
		);
		return true;
	}

	private MutationExecutor executor(SharedSessionContractImplementor session, MutationOperationGroup group, boolean dynamicUpdate) {
		return mutationExecutorService
				.createExecutor( resolveBatchKeyAccess( dynamicUpdate, session ), group, session );
	}

	protected static TableInclusionChecker getTableInclusionChecker(InsertValuesAnalysis insertValuesAnalysis) {
		return tableMapping -> !tableMapping.isOptional()
			|| insertValuesAnalysis.hasNonNullBindings( tableMapping );
	}


	/**
	 * Transform the array of property indexes to an array of booleans,
	 * true when the property is insertable and non-null
	 */
	public boolean[] getPropertiesToInsert(Object[] fields) {
		final var notNull = new boolean[fields.length];
		final var insertable = entityPersister().getPropertyInsertability();
		for ( int i = 0; i < fields.length; i++ ) {
			notNull[i] = insertable[i] && fields[i] != null;
		}
		return notNull;
	}

	protected MutationOperationGroup generateDynamicInsertSqlGroup(
			boolean[] insertable,
			Object object,
			SharedSessionContractImplementor session,
			boolean forceIdentifierBinding) {
		final var insertGroupBuilder = new MutationGroupBuilder( MutationType.INSERT, entityPersister() );
		entityPersister().forEachMutableTable(
				(tableMapping) -> insertGroupBuilder.addTableDetailsBuilder( createTableInsertBuilder( tableMapping, forceIdentifierBinding ) )
		);
		applyTableInsertDetails( insertGroupBuilder, insertable, object, session, forceIdentifierBinding );
		return createOperationGroup( null, insertGroupBuilder.buildMutationGroup() );
	}

	public MutationOperationGroup generateStaticOperationGroup() {
		final var persister = entityPersister();
		final var insertGroupBuilder = new MutationGroupBuilder( MutationType.INSERT, persister );
		persister.forEachMutableTable(
				(tableMapping) -> insertGroupBuilder.addTableDetailsBuilder( createTableInsertBuilder( tableMapping, false ) )
		);
		applyTableInsertDetails( insertGroupBuilder, persister.getPropertyInsertability(), null, null, false );
		return createOperationGroup( null, insertGroupBuilder.buildMutationGroup() );
	}

	private TableMutationBuilder<?> createTableInsertBuilder(EntityTableMapping tableMapping, boolean forceIdentifierBinding) {
		final var persister = entityPersister();
		final var delegate = persister.getInsertDelegate();
		return tableMapping.isIdentifierTable()
			&& delegate != null
			&& !forceIdentifierBinding
				? delegate.createTableMutationBuilder( tableMapping.getInsertExpectation(), factory() )
				: new TableInsertBuilderStandard( persister, tableMapping, factory() );
	}

	private void applyTableInsertDetails(
			MutationGroupBuilder insertGroupBuilder,
			boolean[] attributeInclusions,
			Object object,
			SharedSessionContractImplementor session,
			boolean forceIdentifierBinding) {
		final var attributeMappings = entityPersister().getAttributeMappings();

		insertGroupBuilder.forEachTableMutationBuilder( (builder) -> {
			final var tableMapping = (EntityTableMapping) builder.getMutatingTable().getTableMapping();
			assert !tableMapping.isInverse();

			// `attributeIndexes` represents the indexes (relative to `attributeMappings`) of
			// the attributes mapped to the table
			final int[] attributeIndexes = tableMapping.getAttributeIndexes();
			for ( int i = 0; i < attributeIndexes.length; i++ ) {
				final int attributeIndex = attributeIndexes[ i ];
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
		entityPersister().addSoftDeleteToInsertGroup( insertGroupBuilder );
		entityPersister().addTemporalToInsertGroup( insertGroupBuilder );

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
							tableInsertBuilder.addKeyColumn( valueExpression, keyMapping.getKeyColumn( i ) );
						}
					}
				}
				else if ( generator.referenceColumnsInSql( dialect, EventType.INSERT ) ) {
					if ( columnValues != null ) {
						assert columnValues.length == 1;
						assert keyColumnCount == 1;
						tableInsertBuilder.addKeyColumn( columnValues[0], keyMapping.getKeyColumn( 0 ) );
					}
				}
			}
			else {
				keyMapping.forEachKeyColumn( tableInsertBuilder::addKeyColumn );
			}
		} );
	}

	private static boolean needsValueBinding(OnExecutionGenerator generator, Dialect dialect) {
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
	@Deprecated
	public BasicBatchKey getInsertBatchKey() {
		return batchKey;
	}
}
