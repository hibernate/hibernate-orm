/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import java.sql.SQLException;
import java.util.ArrayList;
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
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableMutationBuilder;
import org.hibernate.tuple.entity.EntityMetamodel;

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

		if ( entityPersister.isIdentifierAssignedByInsert() || entityPersister.hasInsertGeneratedProperties() ) {
			// disable batching in case of insert generated identifier or properties
			batchKey = null;
		}
		else {
			batchKey = new BasicBatchKey( entityPersister.getEntityName() + "#INSERT" );
		}

		if ( entityPersister.getEntityMetamodel().isDynamicInsert() ) {
			// the entity specified dynamic-insert - skip generating the
			// static inserts as we will create them every time
			staticInsertGroup = null;
		}
		else {
			staticInsertGroup = generateStaticOperationGroup();
		}
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
		final EntityPersister persister = entityPersister();
		final boolean forceIdentifierBinding = persister.getGenerator().generatedOnExecution() && id != null;
		if ( persister.getEntityMetamodel().isDynamicInsert()
				|| needsDynamicInsert || forceIdentifierBinding ) {
			return doDynamicInserts( id, values, entity, session, forceIdentifierBinding );
		}
		else {
			return doStaticInserts( id, values, entity, session );
		}
	}

	protected boolean preInsertInMemoryValueGeneration(Object[] values, Object entity, SharedSessionContractImplementor session) {
		final EntityPersister persister = entityPersister();
		final EntityMetamodel entityMetamodel = persister.getEntityMetamodel();
		boolean foundStateDependentGenerator = false;
		if ( entityMetamodel.hasPreInsertGeneratedValues() ) {
			final Generator[] generators = entityMetamodel.getGenerators();
			for ( int i = 0; i < generators.length; i++ ) {
				final Generator generator = generators[i];
				if ( generator != null
						&& generator.generatesOnInsert()
						&& generator.generatedBeforeExecution( entity, session ) ) {
					values[i] = ( (BeforeExecutionGenerator) generator ).generate( session, entity, values[i], INSERT );
					persister.setPropertyValue( entity, i, values[i] );
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
		final InsertValuesAnalysis insertValuesAnalysis = new InsertValuesAnalysis( entityPersister(), values );

		final TableInclusionChecker tableInclusionChecker = getTableInclusionChecker( insertValuesAnalysis );

		final MutationExecutor mutationExecutor = executor( session, staticInsertGroup, false );

		decomposeForInsert(
				mutationExecutor,
				id,
				values,
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
			MutationOperationGroup mutationGroup,
			boolean[] propertyInclusions,
			TableInclusionChecker tableInclusionChecker,
			SharedSessionContractImplementor session) {
		final JdbcValueBindings jdbcValueBindings = mutationExecutor.getJdbcValueBindings();
		final AttributeMappingsList attributeMappings = entityPersister().getAttributeMappings();

		for ( int position = 0; position < mutationGroup.getNumberOfOperations(); position++ ) {
			final MutationOperation operation = mutationGroup.getOperation( position );
			final EntityTableMapping tableDetails = (EntityTableMapping) operation.getTableDetails();
			if ( tableInclusionChecker.include( tableDetails ) ) {
				final int[] attributeIndexes = tableDetails.getAttributeIndexes();
				for ( int i = 0; i < attributeIndexes.length; i++ ) {
					final int attributeIndex = attributeIndexes[ i ];
					if ( propertyInclusions[attributeIndex] ) {
						final AttributeMapping mapping = attributeMappings.get( attributeIndex );
						decomposeAttribute( values[attributeIndex], session, jdbcValueBindings, mapping );
					}
				}
			}
		}

		if ( id == null ) {
			assert entityPersister().getInsertDelegate() != null;
		}
		else {
			for ( int position = 0; position < mutationGroup.getNumberOfOperations(); position++ ) {
				final MutationOperation jdbcOperation = mutationGroup.getOperation( position );
				final EntityTableMapping tableDetails = (EntityTableMapping) jdbcOperation.getTableDetails();
				breakDownJdbcValue( id, session, jdbcValueBindings, tableDetails );
			}
		}
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

	protected void decomposeAttribute(
			Object value,
			SharedSessionContractImplementor session,
			JdbcValueBindings jdbcValueBindings,
			AttributeMapping mapping) {
		if ( !(mapping instanceof PluralAttributeMapping) ) {
			mapping.decompose(
					value,
					0,
					jdbcValueBindings,
					null,
					(valueIndex, bindings, noop, jdbcValue, selectableMapping) -> {
						if ( selectableMapping.isInsertable() ) {
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

	protected GeneratedValues doDynamicInserts(
			Object id,
			Object[] values,
			Object object,
			SharedSessionContractImplementor session,
			boolean forceIdentifierBinding) {
		final boolean[] insertability = getPropertiesToInsert( values );
		final MutationOperationGroup insertGroup =
				generateDynamicInsertSqlGroup( insertability, object, session, forceIdentifierBinding );

		final MutationExecutor mutationExecutor = executor( session, insertGroup, true );

		final InsertValuesAnalysis insertValuesAnalysis = new InsertValuesAnalysis( entityPersister(), values );

		final TableInclusionChecker tableInclusionChecker = getTableInclusionChecker( insertValuesAnalysis );

		decomposeForInsert( mutationExecutor, id, values, insertGroup, insertability, tableInclusionChecker, session );

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
		return tableMapping -> !tableMapping.isOptional() || insertValuesAnalysis.hasNonNullBindings( tableMapping );
	}


	/**
	 * Transform the array of property indexes to an array of booleans,
	 * true when the property is insertable and non-null
	 */
	public boolean[] getPropertiesToInsert(Object[] fields) {
		boolean[] notNull = new boolean[fields.length];
		boolean[] insertable = entityPersister().getPropertyInsertability();
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
		final MutationGroupBuilder insertGroupBuilder = new MutationGroupBuilder( MutationType.INSERT, entityPersister() );
		entityPersister().forEachMutableTable(
				(tableMapping) -> insertGroupBuilder.addTableDetailsBuilder( createTableInsertBuilder( tableMapping, forceIdentifierBinding ) )
		);
		applyTableInsertDetails( insertGroupBuilder, insertable, object, session, forceIdentifierBinding );
		return createOperationGroup( null, insertGroupBuilder.buildMutationGroup() );
	}

	public MutationOperationGroup generateStaticOperationGroup() {
		final MutationGroupBuilder insertGroupBuilder = new MutationGroupBuilder( MutationType.INSERT, entityPersister() );
		entityPersister().forEachMutableTable(
				(tableMapping) -> insertGroupBuilder.addTableDetailsBuilder( createTableInsertBuilder( tableMapping, false ) )
		);
		applyTableInsertDetails( insertGroupBuilder, entityPersister().getPropertyInsertability(), null, null, false );
		return createOperationGroup( null, insertGroupBuilder.buildMutationGroup() );
	}

	private TableMutationBuilder<?> createTableInsertBuilder(EntityTableMapping tableMapping, boolean forceIdentifierBinding) {
		final GeneratedValuesMutationDelegate delegate = entityPersister().getInsertDelegate();
		if ( tableMapping.isIdentifierTable() && delegate != null && !forceIdentifierBinding ) {
			return delegate.createTableMutationBuilder( tableMapping.getInsertExpectation(), factory() );
		}
		else {
			return new TableInsertBuilderStandard( entityPersister(), tableMapping, factory() );
		}
	}

	private void applyTableInsertDetails(
			MutationGroupBuilder insertGroupBuilder,
			boolean[] attributeInclusions,
			Object object,
			SharedSessionContractImplementor session,
			boolean forceIdentifierBinding) {
		final AttributeMappingsList attributeMappings = entityPersister().getAttributeMappings();

		insertGroupBuilder.forEachTableMutationBuilder( (builder) -> {
			final EntityTableMapping tableMapping = (EntityTableMapping) builder.getMutatingTable().getTableMapping();
			assert !tableMapping.isInverse();

			// `attributeIndexes` represents the indexes (relative to `attributeMappings`) of
			// the attributes mapped to the table
			final int[] attributeIndexes = tableMapping.getAttributeIndexes();
			for ( int i = 0; i < attributeIndexes.length; i++ ) {
				final int attributeIndex = attributeIndexes[ i ];
				final AttributeMapping attributeMapping = attributeMappings.get( attributeIndex );
				if ( attributeInclusions[attributeIndex] ) {
					attributeMapping.forEachInsertable( insertGroupBuilder );
				}
				else {
					final Generator generator = attributeMapping.getGenerator();
					if ( isValueGenerated( generator ) ) {
						if ( session != null && generator.generatedBeforeExecution( object, session ) ) {
							attributeInclusions[attributeIndex] = true;
							attributeMapping.forEachInsertable( insertGroupBuilder );
						}
						else if ( isValueGenerationInSql( generator, factory.getJdbcServices().getDialect() ) ) {
							handleValueGeneration( attributeMapping, insertGroupBuilder, (OnExecutionGenerator) generator );
						}
					}
				}
			}
		} );

		// add the discriminator
		entityPersister().addDiscriminatorToInsertGroup( insertGroupBuilder );
		entityPersister().addSoftDeleteToInsertGroup( insertGroupBuilder );

		// add the keys
		insertGroupBuilder.forEachTableMutationBuilder( (tableMutationBuilder) -> {
			final TableInsertBuilder tableInsertBuilder = (TableInsertBuilder) tableMutationBuilder;
			final EntityTableMapping tableMapping = (EntityTableMapping) tableInsertBuilder.getMutatingTable().getTableMapping();
			if ( tableMapping.isIdentifierTable() && entityPersister().isIdentifierAssignedByInsert() && !forceIdentifierBinding ) {
				assert entityPersister().getInsertDelegate() != null;
				final OnExecutionGenerator generator = (OnExecutionGenerator) entityPersister().getGenerator();
				if ( generator.referenceColumnsInSql( dialect ) ) {
					final String[] columnValues = generator.getReferencedColumnValues( dialect );
					if ( columnValues != null ) {
						assert columnValues.length == 1;
						assert tableMapping.getKeyMapping().getColumnCount() == 1;
						tableInsertBuilder.addKeyColumn( columnValues[0], tableMapping.getKeyMapping().getKeyColumn( 0 ) );
					}
				}
			}
			else {
				tableMapping.getKeyMapping().forEachKeyColumn( (TableDetails.KeyColumnConsumer) tableInsertBuilder::addKeyColumn );
			}
		} );
	}

	private static boolean isValueGenerated(Generator generator) {
		return generator != null
			&& generator.generatesOnInsert()
			&& generator.generatedOnExecution();
	}

	private static boolean isValueGenerationInSql(Generator generator, Dialect dialect) {
		assert isValueGenerated( generator );
		return ( (OnExecutionGenerator) generator ).referenceColumnsInSql(dialect);
	}

	/**
	 * @deprecated Use {@link #getBatchKey()}
	 */
	@Deprecated
	public BasicBatchKey getInsertBatchKey() {
		return batchKey;
	}
}
