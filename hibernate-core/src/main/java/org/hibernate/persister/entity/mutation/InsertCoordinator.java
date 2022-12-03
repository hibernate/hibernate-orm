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
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.insert.InsertGeneratedIdentifierDelegate;
import org.hibernate.metamodel.mapping.AttributeMapping;
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
import org.hibernate.generator.Generator;
import org.hibernate.generator.InDatabaseGenerator;
import org.hibernate.generator.InMemoryGenerator;
import org.hibernate.tuple.entity.EntityMetamodel;

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
	private final BasicBatchKey insertBatchKey;

	public InsertCoordinator(AbstractEntityPersister entityPersister, SessionFactoryImplementor factory) {
		super( entityPersister, factory );

		insertBatchKey = new BasicBatchKey(
				entityPersister.getEntityName() + "#INSERT",
				null
		);

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
				Generator generator = generators[i];
				if ( generator != null
						&& !generator.generatedByDatabase()
						&& generator.generatedOnInsert() ) {
					values[i] = ( (InMemoryGenerator) generator ).generate( session, entity, values[i] );
					entityPersister().setPropertyValue( entity, i, values[i] );
				}
			}
		}
	}

	private static class InsertValuesAnalysis implements ValuesAnalysis {
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

	private Object doStaticInserts(Object id, Object[] values, Object object, SharedSessionContractImplementor session) {
		final InsertValuesAnalysis insertValuesAnalysis = new InsertValuesAnalysis( entityPersister(), values );

		final TableInclusionChecker tableInclusionChecker = getTableInclusionChecker( insertValuesAnalysis );

		final MutationExecutorService mutationExecutorService = session.getSessionFactory()
				.getServiceRegistry()
				.getService( MutationExecutorService.class );

		final MutationExecutor mutationExecutor = mutationExecutorService.createExecutor(
				() -> insertBatchKey,
				staticInsertGroup,
				session
		);

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

	private void decomposeForInsert(
			MutationExecutor mutationExecutor,
			Object id,
			Object[] values,
			MutationOperationGroup mutationGroup,
			boolean[] propertyInclusions,
			TableInclusionChecker tableInclusionChecker,
			SharedSessionContractImplementor session) {
		final JdbcValueBindings jdbcValueBindings = mutationExecutor.getJdbcValueBindings();

		final List<AttributeMapping> attributeMappings = entityPersister().getAttributeMappings();
		mutationGroup.forEachOperation( (position, operation) -> {
			final EntityTableMapping tableDetails = (EntityTableMapping) operation.getTableDetails();

			if ( !tableInclusionChecker.include( tableDetails ) ) {
				return;
			}

			final int[] attributeIndexes = tableDetails.getAttributeIndexes();
			for ( int i = 0; i < attributeIndexes.length; i++ ) {
				final int attributeIndex = attributeIndexes[ i ];

				if ( !propertyInclusions[attributeIndex] ) {
					continue;
				}

				final AttributeMapping attributeMapping = attributeMappings.get( attributeIndex );
				if ( attributeMapping instanceof PluralAttributeMapping ) {
					continue;
				}

				attributeMapping.decompose(
						values[ attributeIndex ],
						(jdbcValue, selectableMapping) -> {
							if ( !selectableMapping.isInsertable() ) {
								return;
							}

							final String tableName = entityPersister().physicalTableNameForMutation( selectableMapping );
							jdbcValueBindings.bindValue(
									jdbcValue,
									tableName,
									selectableMapping.getSelectionExpression(),
									ParameterUsage.SET,
									session
							);
						},
						session
				);
			}
		} );

		mutationGroup.forEachOperation( (position, jdbcOperation) -> {
			final EntityTableMapping tableDetails = (EntityTableMapping) jdbcOperation.getTableDetails();

			final String tableName = tableDetails.getTableName();

			if ( id == null )  {
				assert entityPersister().getIdentityInsertDelegate() != null;
				return;
			}

			tableDetails.getKeyMapping().breakDownKeyJdbcValues(
					id,
					(jdbcValue, columnMapping) -> jdbcValueBindings.bindValue(
							jdbcValue,
							tableName,
							columnMapping.getColumnName(),
							ParameterUsage.SET,
							session
					),
					session
			);
		} );
	}

	private Object doDynamicInserts(Object id, Object[] values, Object object, SharedSessionContractImplementor session) {
		final boolean[] insertability = getPropertiesToInsert( values );
		final MutationOperationGroup insertGroup = generateDynamicInsertSqlGroup( insertability );

		final MutationExecutorService mutationExecutorService = session
				.getFactory()
				.getServiceRegistry()
				.getService( MutationExecutorService.class );
		final MutationExecutor mutationExecutor = mutationExecutorService.createExecutor(
				() -> insertBatchKey,
				insertGroup,
				session
		);

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

	private static TableInclusionChecker getTableInclusionChecker(InsertValuesAnalysis insertValuesAnalysis) {
		return (tableMapping) -> !tableMapping.isOptional() || insertValuesAnalysis.hasNonNullBindings( tableMapping );
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

	private MutationOperationGroup generateDynamicInsertSqlGroup(boolean[] insertable) {
		assert entityPersister().getEntityMetamodel().isDynamicInsert();

		final MutationGroupBuilder insertGroupBuilder = new MutationGroupBuilder( MutationType.INSERT, entityPersister() );

		entityPersister().forEachMutableTable( (tableMapping) -> {
			final TableInsertBuilder tableInsertBuilder;
			final InsertGeneratedIdentifierDelegate identityDelegate = entityPersister().getIdentityInsertDelegate();
			if ( tableMapping.isIdentifierTable() && identityDelegate != null ) {
				final BasicEntityIdentifierMapping identifierMapping = (BasicEntityIdentifierMapping) entityPersister().getIdentifierMapping();
				tableInsertBuilder = identityDelegate.createTableInsertBuilder(
						identifierMapping,
						tableMapping.getInsertExpectation(),
						factory()
				);
			}
			else {
				tableInsertBuilder = new TableInsertBuilderStandard(
						entityPersister(),
						tableMapping,
						factory()
				);
			}
			insertGroupBuilder.addTableDetailsBuilder( tableInsertBuilder );
		} );

		applyTableInsertDetails( insertGroupBuilder, insertable );

		return createOperationGroup( null, insertGroupBuilder.buildMutationGroup() );
	}

	public MutationOperationGroup generateStaticOperationGroup() {
		final MutationGroupBuilder insertGroupBuilder = new MutationGroupBuilder( MutationType.INSERT, entityPersister() );

		entityPersister().forEachMutableTable( (tableMapping) -> {
			final TableInsertBuilder tableInsertBuilder;
			final InsertGeneratedIdentifierDelegate identityDelegate = entityPersister().getIdentityInsertDelegate();
			if ( tableMapping.isIdentifierTable() && identityDelegate != null ) {
				tableInsertBuilder = identityDelegate.createTableInsertBuilder(
						(BasicEntityIdentifierMapping) entityPersister().getIdentifierMapping(),
						tableMapping.getInsertExpectation(),
						factory()
				);
			}
			else {
				tableInsertBuilder = new TableInsertBuilderStandard(
						entityPersister(),
						tableMapping,
						factory()
				);
			}
			insertGroupBuilder.addTableDetailsBuilder( tableInsertBuilder );
		} );

		applyTableInsertDetails( insertGroupBuilder, entityPersister().getPropertyInsertability() );

		return createOperationGroup( null, insertGroupBuilder.buildMutationGroup() );
	}

	private void applyTableInsertDetails(
			MutationGroupBuilder insertGroupBuilder,
			boolean[] attributeInclusions) {
		final List<AttributeMapping> attributeMappings = entityPersister().getAttributeMappings();

		insertGroupBuilder.forEachTableMutationBuilder( (builder) -> {
			final EntityTableMapping tableMapping = (EntityTableMapping) builder.getMutatingTable().getTableMapping();
			assert !tableMapping.isInverse();

			// `attributeIndexes` represents the indexes (relative to `attributeMappings`) of
			// the attributes mapped to the table
			final int[] attributeIndexes = tableMapping.getAttributeIndexes();
			for ( int i = 0; i < attributeIndexes.length; i++ ) {
				final int attributeIndex = attributeIndexes[ i ];
				final AttributeMapping attributeMapping = attributeMappings.get( attributeIndex );

				if ( !attributeInclusions[ attributeIndex ] ) {
					final Generator generator = attributeMapping.getGenerator();
					if ( isValueGenerationInSql( generator, factory().getJdbcServices().getDialect()) ) {
						handleValueGeneration( attributeMapping, insertGroupBuilder, (InDatabaseGenerator) generator );
					}
					continue;
				}

				attributeMapping.forEachSelectable( (selectionIndex, selectableMapping) -> {
					if ( selectableMapping.isFormula() ) {
						// no physical column
						return;
					}

					if ( !selectableMapping.isInsertable() ) {
						return;
					}

					final String tableNameForMutation = entityPersister().physicalTableNameForMutation( selectableMapping );
					final TableInsertBuilder tableInsertBuilder = insertGroupBuilder.findTableDetailsBuilder( tableNameForMutation );

					tableInsertBuilder.addValueColumn( selectableMapping );
				} );
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
			&& generator.generatedOnInsert()
			&& generator.generatedByDatabase()
			&& ( (InDatabaseGenerator) generator ).referenceColumnsInSql(dialect);
	}
}
