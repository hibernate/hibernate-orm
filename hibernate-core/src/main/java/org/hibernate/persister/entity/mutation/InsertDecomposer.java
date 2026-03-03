/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.internal.EntityInsertAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.StatementShapeKey;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.metamodel.mapping.AttributeMappingsList;
import org.hibernate.action.queue.Helper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableMutationBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * @author Steve Ebersole
 */
public class InsertDecomposer extends AbstractDecomposer<EntityInsertAction> {
	private final MutationOperationGroup staticInsertGroup;
	private final BasicBatchKey batchKey;

	public InsertDecomposer(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		super( entityPersister, sessionFactory );

		this.staticInsertGroup = entityPersister.isDynamicInsert()
				// the entity specified dynamic-insert - skip generating the
				// static inserts as we will create them every time
				? null
				: generateStaticOperationGroup();

		batchKey = entityPersister.isIdentifierAssignedByInsert() || entityPersister.hasInsertGeneratedProperties()
				// disable batching in case of insert-generated identifier or properties
				? null
				: new BasicBatchKey( entityPersister.getEntityName() + "#INSERT" );
	}

	public MutationOperationGroup getStaticMutationGroup() {
		return staticInsertGroup;
	}

	@Override
	public List<PlannedOperationGroup> decompose(
			EntityInsertAction action,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		final Object entity = action.getInstance();
		final Object identifier = action.getId();
		final Object[] state = action.getState();

		var valuesAnalysis = new InsertValuesAnalysis(entityPersister, state);
		var inclusionChecker = calculateTableInclusionCheck( valuesAnalysis );

		var insertable = entityPersister.getPropertyInsertability();

		var effectiveGroup = chooseEffectiveInsertGroup( insertable, entity, identifier, session );

		LinkedHashMap<String, List<PlannedOperation>> byTable = new LinkedHashMap<>();
		int localOrd = 0;

		for (int i = 0; i < effectiveGroup.getNumberOfOperations(); i++) {
			var operation = effectiveGroup.getOperation(i);
			var table = (EntityTableMapping) operation.getTableDetails();
			String tableName = table.getTableName();

			final BindPlan bindPlan = new InsertBindPlan(
					entityPersister,
					entity,
					identifier,
					state,
					insertable,
					valuesAnalysis,
					inclusionChecker,
					action::getId
			);

			final PlannedOperation op = new PlannedOperation(
					tableName,
					MutationKind.INSERT,
					operation,
					bindPlan,
					ordinalBase * 1_000 + (localOrd++),
					"EntityInsertAction(" + entityPersister.getEntityName() + ")"
			);

			op.setEntityIdSupplier( action::getId );

			byTable.computeIfAbsent(tableName, t -> new ArrayList<>()).add(op);
		}

		ArrayList<PlannedOperationGroup> out = arrayList(byTable.size());
		int ord = 0;
		for (var e : byTable.entrySet()) {
			String tableName = e.getKey();
			List<PlannedOperation> plannedOperations = e.getValue();

			out.add(new PlannedOperationGroup(
					tableName,
					MutationKind.INSERT,
					// hash based on op shape
					StatementShapeKey.forInsert(tableName, plannedOperations),
					plannedOperations,
					Helper.needsIdentityPrePhase(entityPersister, identifier),
					ordinalBase * 1_000 + (ord++),
					"EntityInsertAction(" + entityPersister.getEntityName() + ")"
			));
		}

		return out;
	}

	private TableInclusionChecker calculateTableInclusionCheck(InsertValuesAnalysis analysis) {
		return (tableMapping) -> !tableMapping.isOptional() || analysis.hasNonNullBindings( tableMapping );
	}

	private MutationOperationGroup chooseEffectiveInsertGroup(
			boolean[] insertable,
			Object entity,
			Object id,
			SharedSessionContractImplementor session) {
		final boolean forceIdentifierBinding = entityPersister.getGenerator().generatedOnExecution() && id != null;
		return entityPersister.isDynamicInsert() || forceIdentifierBinding
				? generateDynamicInsertSqlGroup( insertable, entity, session, forceIdentifierBinding )
				: staticInsertGroup;
	}

	public MutationOperationGroup generateStaticOperationGroup() {
		final var insertGroupBuilder = new MutationGroupBuilder( MutationType.INSERT, entityPersister );
		entityPersister.forEachMutableTable( (tableMapping) -> {
			insertGroupBuilder.addTableDetailsBuilder( createTableInsertBuilder( tableMapping, false ) );
		} );
		applyTableInsertDetails( insertGroupBuilder, entityPersister.getPropertyInsertability(), null, null, false );
		return createOperationGroup( null, insertGroupBuilder.buildMutationGroup() );
	}

	protected MutationOperationGroup generateDynamicInsertSqlGroup(
			boolean[] insertable,
			Object object,
			SharedSessionContractImplementor session,
			boolean forceIdentifierBinding) {
		final var insertGroupBuilder = new MutationGroupBuilder( MutationType.INSERT, entityPersister );
		entityPersister.forEachMutableTable(
				(tableMapping) -> insertGroupBuilder.addTableDetailsBuilder( createTableInsertBuilder( tableMapping, forceIdentifierBinding ) )
		);
		applyTableInsertDetails( insertGroupBuilder, insertable, object, session, forceIdentifierBinding );
		return createOperationGroup( null, insertGroupBuilder.buildMutationGroup() );
	}

	private TableMutationBuilder<?> createTableInsertBuilder(EntityTableMapping tableMapping, boolean forceIdentifierBinding) {
		final var delegate = entityPersister.getInsertDelegate();
		return tableMapping.isIdentifierTable()
			&& delegate != null
			&& !forceIdentifierBinding
				? delegate.createTableMutationBuilder( tableMapping.getInsertExpectation(), sessionFactory )
				: new TableInsertBuilderStandard( entityPersister, tableMapping, sessionFactory );
	}

	private void applyTableInsertDetails(
			MutationGroupBuilder insertGroupBuilder,
			boolean[] attributeInclusions,
			Object object,
			SharedSessionContractImplementor session,
			boolean forceIdentifierBinding) {
		final AttributeMappingsList attributeMappings = entityPersister.getAttributeMappings();
		final Dialect dialect = sessionFactory.getJdbcServices().getDialect();

		insertGroupBuilder.forEachTableMutationBuilder( (builder) -> {
			final var tableMapping = (EntityTableMapping) builder.getMutatingTable().getTableMapping();
			assert !tableMapping.isInverse();

			// `attributeIndexes` represents the indexes (relative to `attributeMappings`) of
			// the attributes mapped to the table
			final int[] attributeIndexes = tableMapping.getAttributeIndexes();
			for ( int i = 0; i < attributeIndexes.length; i++ ) {
				final int attributeIndex = attributeIndexes[ i ];
				final var attributeMapping = attributeMappings.get( attributeIndex );
				if ( attributeInclusions[attributeIndex] ) {
					attributeMapping.forEachInsertable( insertGroupBuilder );
				}
				else {
					final var generator = attributeMapping.getGenerator();
					if ( isValueGenerated( generator ) ) {
						if ( session != null && generator.generatedBeforeExecution( object, session ) ) {
							attributeInclusions[attributeIndex] = true;
							attributeMapping.forEachInsertable( insertGroupBuilder );
						}
						else if ( isValueGenerationInSql( generator, dialect ) ) {
							handleValueGeneration( attributeMapping, insertGroupBuilder, (OnExecutionGenerator) generator );
						}
					}
				}
			}
		} );

		entityPersister.addDiscriminatorToInsertGroup( insertGroupBuilder );
		entityPersister.addSoftDeleteToInsertGroup( insertGroupBuilder );

		// add the keys
		insertGroupBuilder.forEachTableMutationBuilder( (tableMutationBuilder) -> {
			final var tableInsertBuilder = (TableInsertBuilder) tableMutationBuilder;
			final var tableMapping = (EntityTableMapping) tableInsertBuilder.getMutatingTable().getTableMapping();
			final var keyMapping = tableMapping.getKeyMapping();
			if ( tableMapping.isIdentifierTable() && entityPersister.isIdentifierAssignedByInsert() && !forceIdentifierBinding ) {
				assert entityPersister.getInsertDelegate() != null;
				final var generator = (OnExecutionGenerator) entityPersister.getGenerator();
				if ( generator.referenceColumnsInSql( dialect ) ) {
					final String[] columnValues = generator.getReferencedColumnValues( dialect );
					if ( columnValues != null ) {
						assert columnValues.length == 1;
						assert keyMapping.getColumnCount() == 1;
						tableInsertBuilder.addKeyColumn( columnValues[0], keyMapping.getKeyColumn( 0 ) );
					}
				}
			}
			else {
				keyMapping.forEachKeyColumn( tableInsertBuilder::addKeyColumn );
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
		return ( (OnExecutionGenerator) generator ).referenceColumnsInSql( dialect );
	}


}
