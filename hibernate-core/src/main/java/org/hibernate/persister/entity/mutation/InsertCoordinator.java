/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.persister.entity.mutation;

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
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.tuple.entity.EntityMetamodel;

import static org.hibernate.generator.EventType.INSERT;

/**
 * Coordinates the insertion of an entity.
 *
 * @see #coordinateInsert
 *
 * @author Steve Ebersole
 */
@Internal
public class InsertCoordinator extends AbstractMutationCoordinator {
	private final MutationOperationGroup staticInsertGroup;
	private final BasicBatchKey batchKey;

	public InsertCoordinator(AbstractEntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );

		if ( entityPersister.hasInsertGeneratedProperties() ) {
			// disable batching in case of insert generated properties
			batchKey = null;
		}
		else {
			batchKey = new BasicBatchKey(
					entityPersister.getEntityName() + "#INSERT",
					null
			);
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

	public MutationOperationGroup getStaticInsertGroup() {
		return staticInsertGroup;
	}

	@Override
	protected BatchKey getBatchKey() {
		return batchKey;
	}

	/**
	 * Perform the insert(s).
	 *
	 * @param id This is the id as known in memory.  For post-insert id generation (IDENTITY, etc)
	 * 		this will be null.
	 * @param values The extracted attribute values
	 * @param entity The entity instance being persisted
	 * @param session The originating context
	 *
	 * @return The id
	 */
	public Object coordinateInsert(
			Object id,
			Object[] values,
			Object entity,
			SharedSessionContractImplementor session) {
		// apply any pre-insert in-memory value generation
		preInsertInMemoryValueGeneration( values, entity, session );

		final EntityMetamodel entityMetamodel = entityPersister().getEntityMetamodel();
		if ( entityMetamodel.isDynamicInsert() ) {
			return doDynamicInserts( id, values, entity, session );
		}
		else {
			return doStaticInserts( id, values, entity, session );
		}
	}

	protected void preInsertInMemoryValueGeneration(Object[] values, Object entity, SharedSessionContractImplementor session) {
		final EntityMetamodel entityMetamodel = entityPersister().getEntityMetamodel();
		if ( entityMetamodel.hasPreInsertGeneratedValues() ) {
			final Generator[] generators = entityMetamodel.getGenerators();
			for ( int i = 0; i < generators.length; i++ ) {
				final Generator generator = generators[i];
				if ( generator != null
						&& !generator.generatedOnExecution()
						&& generator.generatesOnInsert() ) {
					values[i] = ( (BeforeExecutionGenerator) generator ).generate( session, entity, values[i], INSERT );
					entityPersister().setPropertyValue( entity, i, values[i] );
				}
			}
		}
	}

	protected static class InsertValuesAnalysis implements ValuesAnalysis {
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

	protected Object doStaticInserts(Object id, Object[] values, Object object, SharedSessionContractImplementor session) {
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
					(statementDetails, affectedRowCount, batchPosition) -> {
						statementDetails.getExpectation().verifyOutcome(
								affectedRowCount,
								statementDetails.getStatement(),
								batchPosition,
								statementDetails.getSqlString()
						);
						return true;
					},
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

		mutationGroup.forEachOperation( (position, operation) -> {
			final EntityTableMapping tableDetails = (EntityTableMapping) operation.getTableDetails();
			if ( tableInclusionChecker.include( tableDetails ) ) {
				final int[] attributeIndexes = tableDetails.getAttributeIndexes();
				for ( int i = 0; i < attributeIndexes.length; i++ ) {
					final int attributeIndex = attributeIndexes[ i ];
					if ( propertyInclusions[attributeIndex] ) {
						final AttributeMapping mapping = entityPersister().getAttributeMappings().get( attributeIndex );
						decomposeAttribute( values[attributeIndex], session, jdbcValueBindings, mapping );
					}
				}
			}
		} );

		mutationGroup.forEachOperation( (position, jdbcOperation) -> {
			if ( id == null )  {
				assert entityPersister().getIdentityInsertDelegate() != null;
			}
			else {
				final EntityTableMapping tableDetails = (EntityTableMapping) jdbcOperation.getTableDetails();
				breakDownJdbcValue( id, session, jdbcValueBindings, tableDetails );
			}
		} );
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

	protected Object doDynamicInserts(Object id, Object[] values, Object object, SharedSessionContractImplementor session) {
		final boolean[] insertability = getPropertiesToInsert( values );
		final MutationOperationGroup insertGroup = generateDynamicInsertSqlGroup( insertability );

		final MutationExecutor mutationExecutor = executor( session, insertGroup, true );

		final InsertValuesAnalysis insertValuesAnalysis = new InsertValuesAnalysis( entityPersister(), values );

		final TableInclusionChecker tableInclusionChecker = getTableInclusionChecker( insertValuesAnalysis );

		decomposeForInsert( mutationExecutor, id, values, insertGroup, insertability, tableInclusionChecker, session );

		try {
			return mutationExecutor.execute(
					object,
					insertValuesAnalysis,
					tableInclusionChecker,
					(statementDetails, affectedRowCount, batchPosition) -> {
						statementDetails.getExpectation().verifyOutcome(
								affectedRowCount,
								statementDetails.getStatement(),
								batchPosition,
								statementDetails.getSqlString()
						);
						return true;
					},
					session
			);
		}
		finally {
			mutationExecutor.release();
		}
	}

	private MutationExecutor executor(SharedSessionContractImplementor session, MutationOperationGroup group, boolean dynamicUpdate) {
		return session.getFactory()
				.getServiceRegistry()
				.getService( MutationExecutorService.class )
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

	protected MutationOperationGroup generateDynamicInsertSqlGroup(boolean[] insertable) {
		assert entityPersister().getEntityMetamodel().isDynamicInsert();
		final MutationGroupBuilder insertGroupBuilder = new MutationGroupBuilder( MutationType.INSERT, entityPersister() );
		entityPersister().forEachMutableTable(
				(tableMapping) -> insertGroupBuilder.addTableDetailsBuilder( createTableInsertBuilder( tableMapping ) )
		);
		applyTableInsertDetails( insertGroupBuilder, insertable );
		return createOperationGroup( null, insertGroupBuilder.buildMutationGroup() );
	}

	public MutationOperationGroup generateStaticOperationGroup() {
		final MutationGroupBuilder insertGroupBuilder = new MutationGroupBuilder( MutationType.INSERT, entityPersister() );
		entityPersister().forEachMutableTable(
				(tableMapping) -> insertGroupBuilder.addTableDetailsBuilder( createTableInsertBuilder( tableMapping ) )
		);
		applyTableInsertDetails( insertGroupBuilder, entityPersister().getPropertyInsertability() );
		return createOperationGroup( null, insertGroupBuilder.buildMutationGroup() );
	}

	private TableInsertBuilder createTableInsertBuilder(EntityTableMapping tableMapping) {
		final InsertGeneratedIdentifierDelegate identityDelegate = entityPersister().getIdentityInsertDelegate();
		if ( tableMapping.isIdentifierTable() && identityDelegate != null ) {
			final BasicEntityIdentifierMapping mapping =
					(BasicEntityIdentifierMapping) entityPersister().getIdentifierMapping();
			return identityDelegate.createTableInsertBuilder( mapping, tableMapping.getInsertExpectation(), factory() );
		}
		else {
			return new TableInsertBuilderStandard( entityPersister(), tableMapping, factory() );
		}
	}

	private void applyTableInsertDetails(
			MutationGroupBuilder insertGroupBuilder,
			boolean[] attributeInclusions) {
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
					if ( isValueGenerationInSql( generator, factory().getJdbcServices().getDialect() ) ) {
						handleValueGeneration( attributeMapping, insertGroupBuilder, (OnExecutionGenerator) generator );
					}
				}
			}
		} );

		// add the discriminator
		entityPersister().addDiscriminatorToInsertGroup( insertGroupBuilder );

		// add the keys
		final InsertGeneratedIdentifierDelegate identityDelegate = entityPersister().getIdentityInsertDelegate();
		insertGroupBuilder.forEachTableMutationBuilder( (tableMutationBuilder) -> {
			final TableInsertBuilder tableInsertBuilder = (TableInsertBuilder) tableMutationBuilder;
			final EntityTableMapping tableMapping = (EntityTableMapping) tableInsertBuilder.getMutatingTable().getTableMapping();
			//noinspection StatementWithEmptyBody
			if ( tableMapping.isIdentifierTable() && identityDelegate != null ) {
				// nothing to do - the builder already includes the identity handling
			}
			else {
				tableMapping.getKeyMapping().forEachKeyColumn( tableInsertBuilder::addKeyColumn );
			}
		} );
	}

	private static boolean isValueGenerationInSql(Generator generator, Dialect dialect) {
		return generator != null
			&& generator.generatesOnInsert()
			&& generator.generatedOnExecution()
			&& ( (OnExecutionGenerator) generator ).referenceColumnsInSql(dialect);
	}

	/**
	 * @deprecated Use {@link #getBatchKey()}
	 */
	@Deprecated
	public BasicBatchKey getInsertBatchKey() {
		return batchKey;
	}
}
