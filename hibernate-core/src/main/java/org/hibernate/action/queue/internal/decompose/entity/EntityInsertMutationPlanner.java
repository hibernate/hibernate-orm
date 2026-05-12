/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;

import java.util.Collections;
import java.util.Map;

import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.queue.spi.bind.GeneratedValuesCollector;
import org.hibernate.action.queue.spi.decompose.DecompositionContext;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptorAsTableMapping;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableInsert;
import org.hibernate.sql.model.ast.builder.AssigningTableMutationBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;

import static org.hibernate.action.queue.internal.decompose.entity.DecompositionHelper.hasValueGenerationOnExecution;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.temporal.TemporalTableStrategy.SINGLE_TABLE;

/// Shared planner for graph entity insert mutation operations.
///
/// This helper owns the insert-operation building logic used by ordinary insert
/// decomposition and by state-management contributors that need to materialize a
/// replacement insert, such as temporal update handling.
///
/// @author Steve Ebersole
/// @since 8.0
class EntityInsertMutationPlanner {
	private final EntityPersister entityPersister;
	private final SessionFactoryImplementor sessionFactory;
	private final Map<String, TableInsert> staticInsertOperations;

	EntityInsertMutationPlanner(
			EntityPersister entityPersister,
			SessionFactoryImplementor sessionFactory) {
		this.entityPersister = entityPersister;
		this.sessionFactory = sessionFactory;
		this.staticInsertOperations = entityPersister.isDynamicInsert()
				? null
				: generateStaticOperations();
	}

	/// Static set of table mutations used to perform entity creation, or `null`
	/// when the entity is configured for dynamic insert.
	Map<String, TableInsert> getStaticInsertOperations() {
		return staticInsertOperations;
	}

	boolean[] resolveInsertability(Object[] state) {
		return entityPersister.isDynamicInsert()
				? getPropertiesToInsert( state )
				: entityPersister.getPropertyInsertability();
	}

	Map<String, TableInsert> resolveInsertOperations(
			boolean[] effectiveInsertability,
			Object entity,
			Object identifier,
			boolean hasStateDependentGenerator,
			SharedSessionContractImplementor session) {
		return chooseEffectiveInsertGroup(
				effectiveInsertability,
				entity,
				identifier,
				hasStateDependentGenerator,
				session
		);
	}

	boolean preInsertInMemoryValueGeneration(Object[] values, Object entity, SharedSessionContractImplementor session) {
		boolean foundStateDependentGenerator = false;
		if ( entityPersister.hasPreInsertGeneratedProperties() ) {
			final var generators = entityPersister.getGenerators();
			for ( int i = 0; i < generators.length; i++ ) {
				final var generator = generators[i];
				if ( generator != null
						&& generator.generatesOnInsert()
						&& generator.generatedBeforeExecution( entity, session ) ) {
					values[i] = ( (BeforeExecutionGenerator) generator ).generate( session, entity, values[i], INSERT );
					entityPersister.setValue( entity, i, values[i] );
					foundStateDependentGenerator = foundStateDependentGenerator || generator.generatedOnExecution();
				}
			}
		}
		return foundStateDependentGenerator;
	}

	EntityInsertBindPlan createInsertBindPlan(
			EntityTableDescriptor tableDescriptor,
			Object entity,
			Object identifier,
			Object[] state,
			boolean[] insertable,
			AbstractEntityInsertAction action,
			GeneratedValuesCollector generatedValuesCollector,
			DecompositionContext decompositionContext) {
		final EntityTableDescriptor tableDescriptorToUse = entityPersister instanceof UnionSubclassEntityPersister
				? entityPersister.getIdentifierTableDescriptor()
				: tableDescriptor;

		return new EntityInsertBindPlan(
				tableDescriptorToUse,
				entityPersister,
				entity,
				identifier,
				state,
				insertable,
				action,
				generatedValuesCollector,
				decompositionContext
		);
	}

	private Map<String, TableInsert> chooseEffectiveInsertGroup(
			boolean[] effectiveInsertability,
			Object entity,
			Object id,
			boolean hasStateDependentGenerator,
			SharedSessionContractImplementor session) {
		final boolean forceIdentifierBinding = entityPersister.getGenerator().generatedOnExecution() && id != null;
		return entityPersister.isDynamicInsert() || forceIdentifierBinding || hasStateDependentGenerator
				? generateDynamicInsertOperations( effectiveInsertability, entity, session, forceIdentifierBinding )
				: staticInsertOperations;
	}

	private boolean[] getPropertiesToInsert(Object[] fields) {
		final boolean[] notNull = new boolean[fields.length];
		final boolean[] insertable = entityPersister.getPropertyInsertability();
		for ( int i = 0; i < fields.length; i++ ) {
			notNull[i] = insertable[i] && fields[i] != null;
		}
		return notNull;
	}

	private Map<String, TableInsert> generateStaticOperations() {
		final Map<String, TableInsertBuilder> staticOperationBuilders = CollectionHelper.linkedMapOfSize( entityPersister.getTableDescriptors().length );
		entityPersister.forEachMutableTableDescriptor( (tableDescriptor) -> {
			staticOperationBuilders.put(
					tableDescriptor.name(),
					createTableInsertBuilder( tableDescriptor )
			);
		} );

		applyTableInsertDetails(
				staticOperationBuilders,
				entityPersister.getPropertyInsertability(),
				null,
				null,
				false
		);

		final Map<String, TableInsert> staticOperations = CollectionHelper.linkedMapOfSize( staticOperationBuilders.size() );
		staticOperationBuilders.forEach( (name, operationBuilder) -> {
			staticOperations.put( name, operationBuilder.buildMutation() );
		} );
		return Collections.unmodifiableMap( staticOperations );
	}

	private TableInsertBuilder createTableInsertBuilder(TableDescriptor tableDescriptor) {
		final boolean isIdentifierTable = tableDescriptor instanceof EntityTableDescriptor entityTableDescriptor
				&& entityTableDescriptor.isIdentifierTable();

		if ( isIdentifierTable ) {
			final var delegate = entityPersister.getInsertDelegate();
			if ( delegate != null ) {
				return (TableInsertBuilder) delegate.createTableMutationBuilder( null, sessionFactory );
			}
		}

		final boolean isInverse = tableDescriptor instanceof EntityTableDescriptor entityTableDescriptor
				&& entityTableDescriptor.isInverse();
		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				tableDescriptor,
				tableDescriptor.getRelativePosition(),
				isIdentifierTable,
				isInverse
		);

		return new TableInsertBuilderStandard(
				entityPersister,
				new MutatingTableReference( tableMapping ),
				sessionFactory
		);
	}

	private void applyTableInsertDetails(
			Map<String, TableInsertBuilder> builders,
			boolean[] attributeInclusions,
			Object object,
			SharedSessionContractImplementor session,
			boolean forceIdentifierBinding) {
		final Dialect dialect = sessionFactory.getJdbcServices().getDialect();

		entityPersister.forEachMutableTableDescriptor( (tableDescriptor) -> {
			final var builder = builders.get( tableDescriptor.name() );

			for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
				var attribute = tableDescriptor.attributes().get( i );
				final var generator = attribute.getGenerator();
				if ( generator instanceof OnExecutionGenerator onExecutionGenerator
						&& hasValueGenerationOnExecution( onExecutionGenerator, INSERT, object, session, dialect ) ) {
					if ( needsValueBinding( onExecutionGenerator, dialect ) ) {
						attributeInclusions[attribute.getStateArrayPosition()] = true;
					}
					handleValueGeneration( attribute, builder, onExecutionGenerator, INSERT, dialect );
				}
				else if ( attributeInclusions[attribute.getStateArrayPosition()] ) {
					attribute.forEachInsertable( builder );
				}
				else {
					if ( isValueGenerated( generator ) ) {
						if ( session != null && generator.generatedBeforeExecution( object, session ) ) {
							attributeInclusions[attribute.getStateArrayPosition()] = true;
							attribute.forEachInsertable( builder );
						}
						else if ( isValueGenerationInSql( generator, dialect ) ) {
							handleValueGeneration( attribute, builder, (OnExecutionGenerator) generator, dialect );
						}
					}
				}
			}
		} );

		entityPersister.addDiscriminatorToInsertGroup( builders::get );
		entityPersister.addSoftDeleteToInsertGroup( builders::get );
		addTemporalToInsertGroup( builders );
		addKeysToInsertGroup( builders, dialect, forceIdentifierBinding );
	}

	private void addKeysToInsertGroup(
			Map<String, TableInsertBuilder> builders,
			Dialect dialect,
			boolean forceIdentifierBinding) {
		builders.forEach( (name, builder) -> {
			final var tableMapping = builder.getMutatingTable().getTableMapping();
			if ( tableMapping.isIdentifierTable()
					&& entityPersister.isIdentifierAssignedByInsert()
					&& !forceIdentifierBinding ) {
				assert entityPersister.getInsertDelegate() != null;
				final var generator = (OnExecutionGenerator) entityPersister.getGenerator();
				final boolean[] columnInclusions = generator.getColumnInclusions( dialect, INSERT );
				final String[] columnValues = generator.getReferencedColumnValues( dialect, INSERT );
				final var keyColumns = tableMapping.getKeyDetails().getKeyColumns();
				if ( columnInclusions != null ) {
					for ( int i = 0; i < keyColumns.size(); i++ ) {
						if ( columnInclusions[i] ) {
							if ( columnValues != null ) {
								builder.addColumnAssignment( keyColumns.get( i ), columnValues[i] );
							}
							else {
								builder.addColumnAssignment( keyColumns.get( i ) );
							}
						}
					}
				}
				else if ( generator.referenceColumnsInSql( dialect, INSERT ) ) {
					if ( columnValues != null ) {
						for ( int i = 0; i < keyColumns.size(); i++ ) {
							builder.addColumnAssignment( keyColumns.get( i ), columnValues[i] );
						}
					}
					else {
						for ( var keyColumn : keyColumns ) {
							builder.addColumnAssignment( keyColumn );
						}
					}
				}
			}
			else {
				for ( var keyColumn : tableMapping.getKeyDetails().getKeyColumns() ) {
					if ( !builder.hasColumnAssignment( keyColumn ) ) {
						builder.addColumnAssignment( keyColumn );
					}
				}
			}
		} );
	}

	private void addTemporalToInsertGroup(Map<String, TableInsertBuilder> builders) {
		final TemporalMapping temporalMapping = entityPersister.getTemporalMapping();
		if ( temporalMapping == null
				|| sessionFactory.getSessionFactoryOptions().getTemporalTableStrategy() != SINGLE_TABLE ) {
			return;
		}

		final String tableName = entityPersister.physicalTableNameForMutation(
				temporalMapping.getStartingColumnMapping()
		);
		final TableInsertBuilder insertBuilder = builders.get( tableName );
		if ( insertBuilder == null ) {
			return;
		}

		final var startingColumn = new ColumnReference(
				insertBuilder.getMutatingTable(),
				temporalMapping.getStartingColumnMapping()
		);
		insertBuilder.addColumnAssignment( temporalMapping.createStartingValueBinding( startingColumn ) );

		final var endingColumn = new ColumnReference(
				insertBuilder.getMutatingTable(),
				temporalMapping.getEndingColumnMapping()
		);
		insertBuilder.addColumnAssignment( temporalMapping.createNullEndingValueBinding( endingColumn ) );
	}

	private Map<String, TableInsert> generateDynamicInsertOperations(
			boolean[] insertable,
			Object object,
			SharedSessionContractImplementor session,
			boolean forceIdentifierBinding) {
		final Map<String, TableInsertBuilder> operationBuilders = CollectionHelper.linkedMapOfSize( entityPersister.getTableDescriptors().length );
		entityPersister.forEachMutableTableDescriptor( (tableDescriptor) -> {
			operationBuilders.put(
					tableDescriptor.name(),
					createTableInsertBuilder( tableDescriptor )
			);
		} );

		applyTableInsertDetails(
				operationBuilders,
				insertable,
				object,
				session,
				forceIdentifierBinding
		);

		final Map<String, TableInsert> operations = CollectionHelper.linkedMapOfSize( operationBuilders.size() );
		operationBuilders.forEach( (name, operationBuilder) -> {
			operations.put( name, operationBuilder.buildMutation() );
		} );
		return operations;
	}

	private static boolean needsValueBinding(OnExecutionGenerator generator, Dialect dialect) {
		if ( generator.generatesOnInsert() ) {
			final boolean[] columnInclusions = generator.getColumnInclusions( dialect, INSERT );
			final String[] columnValues = generator.getReferencedColumnValues( dialect, INSERT );
			if ( columnValues != null ) {
				for ( int i = 0; i < columnValues.length; i++ ) {
					if ( ( columnInclusions == null || columnInclusions[i] )
							&& "?".equals( columnValues[i] ) ) {
						return true;
					}
				}
				return false;
			}
			else {
				return generator.writePropertyValue( INSERT );
			}
		}
		else {
			return false;
		}
	}

	private static void handleValueGeneration(
			AttributeMapping attributeMapping,
			AssigningTableMutationBuilder<?> builder,
			OnExecutionGenerator generator,
			Dialect dialect) {
		handleValueGeneration( attributeMapping, builder, generator, null, dialect );
	}

	private static void handleValueGeneration(
			AttributeMapping attributeMapping,
			AssigningTableMutationBuilder<?> builder,
			OnExecutionGenerator generator,
			org.hibernate.generator.EventType eventType,
			Dialect dialect) {
		if ( eventType != null ) {
			final String[] columnValues = generator.getReferencedColumnValues( dialect, eventType );
			final boolean[] columnInclusions = generator.getColumnInclusions( dialect, eventType );
			attributeMapping.forEachSelectable( (j, mapping) -> {
				if ( columnInclusions == null || columnInclusions[j] ) {
					final String columnValue = columnValues != null && columnValues[j] != null
							? columnValues[j]
							: "?";
					builder.addColumnAssignment( mapping, columnValue );
				}
			} );
			return;
		}

		final boolean writePropertyValue = generator.writePropertyValue();
		final String[] columnValues = writePropertyValue
				? null
				: generator.getReferencedColumnValues( dialect );
		attributeMapping.forEachSelectable( (j, mapping) -> {
			if ( writePropertyValue ) {
				builder.addColumnAssignment( mapping );
			}
			else {
				builder.addColumnAssignment( mapping, columnValues[j] );
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
