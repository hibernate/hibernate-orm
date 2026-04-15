/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.exec.BindPlan;
import org.hibernate.action.queue.exec.GeneratedValuesCollector;
import org.hibernate.action.queue.decompose.DecompositionContext;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptorAsTableMapping;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.internal.Nullability;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.sql.model.ast.TableInsert;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.builder.TableInsertBuilder;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.action.queue.support.Helper;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hibernate.generator.EventType.INSERT;


/// [Decomposer][EntityActionDecomposer] for entity insert operations.
///
/// Converts an [AbstractEntityInsertAction] into a group of [PlannedOperation] to be performed.
///
/// @author Steve Ebersole
public class InsertDecomposer extends AbstractDecomposer<AbstractEntityInsertAction> {
	private final Map<String, TableInsert> staticInsertOperations;

	public InsertDecomposer(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		super( entityPersister, sessionFactory );

		this.staticInsertOperations = entityPersister.isDynamicInsert()
				// the entity specified dynamic-insert - skip generating the
				// static inserts as we will create them every time
				? null
				: generateStaticOperations();
	}

	public Map<String, TableInsert> getStaticInsertOperations() {
		return staticInsertOperations;
	}

	@Override
	public List<PlannedOperation> decompose(
			AbstractEntityInsertAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext) {
		final boolean vetoed = preInsert( action, session );
		if ( vetoed ) {
			return List.of();
		}

		// Nullify transient references before decomposing - this ensures bidirectional
		// associations are handled correctly and nullability checks are performed
		nullifyTransientReferencesIfNotAlready( action, session, decompositionContext );

		final Object entity = action.getInstance();
		final Object identifier = action.getId();
		final Object[] state = action.getState();

		// apply any pre-insert in-memory value generation
		final boolean hasStateDependentGenerator = preInsertInMemoryValueGeneration( state, entity, session );

		var insertable = entityPersister.getPropertyInsertability();
		var valuesAnalysis = new InsertValuesAnalysis( entityPersister, state );
		var effectiveGroup = chooseEffectiveInsertGroup( insertable, entity, identifier, hasStateDependentGenerator, session );

		final var generatedValuesCollector = GeneratedValuesCollector.forInsert( entityPersister, sessionFactory );
		final PostInsertHandling postInsertHandling = new PostInsertHandling( action, generatedValuesCollector );

		// Compute whether this entity insert needs identity pre-phase
		final boolean needsIdPrePhase = Helper.needsIdentityPrePhase(entityPersister, identifier);

		final List<PlannedOperation> operations = CollectionHelper.arrayList( effectiveGroup.size() );
		int localOrd = 0;
		for ( Map.Entry<String, TableInsert> entry : effectiveGroup.entrySet() ) {
			var operation = entry.getValue().createMutationOperation(valuesAnalysis, sessionFactory);
			var tableMapping = (TableDescriptorAsTableMapping) operation.getTableDetails();
			var tableDescriptor = (EntityTableDescriptor) tableMapping.getDescriptor();

			if ( !valuesAnalysis.include( tableDescriptor ) ) {
				continue;
			}

			final BindPlan bindPlan = createInsertBindPlan(
					tableDescriptor,
					entity,
					identifier,
					state,
					insertable,
					action,
					generatedValuesCollector
			);

			final PlannedOperation op = new PlannedOperation(
					tableDescriptor,
					MutationKind.INSERT,
					operation,
					bindPlan,
					ordinalBase * 1_000 + (localOrd++),
					"EntityInsertAction(" + entityPersister.getEntityName() + ")",
					needsIdPrePhase
			);

			operations.add(op);
		}

		// Attach post-execution callback to the last operation
		if ( !operations.isEmpty() ) {
			operations.get( operations.size() - 1 ).setPostExecutionCallback( postInsertHandling );
		}

		return operations;
	}

	private void nullifyTransientReferencesIfNotAlready(
			AbstractEntityInsertAction action,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext) {
		new ForeignKeys.Nullifier(
				action.getInstance(),
				false,
				action.isEarlyInsert(),
				session,
				entityPersister,
				decompositionContext ).nullifyTransientReferences( action.getState() );
		new Nullability( session, Nullability.NullabilityCheckType.CREATE )
				.checkNullability( action.getState(), entityPersister );
	}

	protected boolean preInsert(AbstractEntityInsertAction action, SharedSessionContractImplementor session) {
		final var listenerGroup = session.getFactory().getEventListenerGroups().eventListenerGroup_PRE_INSERT;
		if ( listenerGroup.isEmpty() ) {
			return false;
		}
		else {
			boolean veto = false;
			final PreInsertEvent event = new PreInsertEvent(
					action.getInstance(),
					action.getId(),
					action.getState(),
					action.getPersister(),
					session
			);
			for ( var listener : listenerGroup.listeners() ) {
				veto |= listener.onPreInsert( event );
			}
			return veto;
		}
	}

	protected boolean preInsertInMemoryValueGeneration(Object[] values, Object entity, SharedSessionContractImplementor session) {
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

	private Map<String, TableInsert> chooseEffectiveInsertGroup(
			boolean[] insertable,
			Object entity,
			Object id,
			boolean hasStateDependentGenerator,
			SharedSessionContractImplementor session) {
		final boolean forceIdentifierBinding = entityPersister.getGenerator().generatedOnExecution() && id != null;
		return entityPersister.isDynamicInsert() || forceIdentifierBinding || hasStateDependentGenerator
				? generateDynamicInsertOperations( insertable, entity, session, forceIdentifierBinding )
				: staticInsertOperations;
	}

	private Map<String, TableInsert> generateStaticOperations() {
		final Map<String, TableInsertBuilder> staticOperationBuilders = CollectionHelper.linkedMapOfSize( entityPersister.getTableDescriptors().length );
		entityPersister.forEachMutableTableDescriptor( (tableDescriptor) -> {
			staticOperationBuilders.put(
					tableDescriptor.name(),
					createTableInsertBuilder(tableDescriptor, false)
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
		staticOperationBuilders.forEach(  (name, operationBuilder) -> {
			staticOperations.put( name, operationBuilder.buildMutation() );
		} );
		return Collections.unmodifiableMap( staticOperations );
	}

	private TableInsertBuilder createTableInsertBuilder(
			TableDescriptor tableDescriptor,
			boolean forceIdentifierBinding) {
		final var delegate = entityPersister.getInsertDelegate();
		// TODO: Handle custom insert delegates
		// For now, always use standard builder

		// Create adapter to convert TableDescriptor to TableMapping
		final boolean isIdentifierTable = tableDescriptor instanceof EntityTableDescriptor etd
				&& etd.isIdentifierTable();
		final TableDescriptorAsTableMapping tableMapping = new TableDescriptorAsTableMapping(
				tableDescriptor,
				0, // relativePosition - will be set correctly by persister
				isIdentifierTable,
				false // isInverse
		);

		return new TableInsertBuilderStandard(
				entityPersister,
				new MutatingTableReference(tableMapping),
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

		builders.forEach( (name, builder) -> {
			// Get the TableDescriptor from the adapter
			final var tableMapping = (TableDescriptorAsTableMapping) builder.getMutatingTable().getTableMapping();
			final var tableDescriptor = (EntityTableDescriptor) tableMapping.getDescriptor();
			// Skip inverse tables
			if (tableDescriptor.isInverse()) {
				return;
			}

			for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
				var attribute = tableDescriptor.attributes().get(i);
				if ( attributeInclusions[attribute.getStateArrayPosition()] ) {
					attribute.forEachInsertable( builder );
				}
				else {
					final var generator = attribute.getGenerator();
					if ( isValueGenerated( generator ) ) {
						if ( session != null && generator.generatedBeforeExecution( object, session ) ) {
							attributeInclusions[attribute.getStateArrayPosition()] = true;
							attribute.forEachInsertable( builder );
						}
						else if ( isValueGenerationInSql( generator, dialect ) ) {
							handleValueGeneration( attribute, builder, (OnExecutionGenerator) generator );
						}
					}
				}
			}
		} );

		// NOTE : unlike the legacy handling, here we use parameters for discriminator to properly plan
		// and batch these insert statements.
		entityPersister.addDiscriminatorToInsertGroup( builders::get );

		entityPersister.addSoftDeleteToInsertGroup( builders::get );

		// add the keys
		builders.forEach( (name, builder) -> {
			// Get the TableDescriptor from the adapter
			final var tableMapping = (TableDescriptorAsTableMapping) builder.getMutatingTable().getTableMapping();
			final var tableDescriptor = (EntityTableDescriptor) tableMapping.getDescriptor();

			if ( tableDescriptor.isIdentifierTable()
					&& entityPersister.isIdentifierAssignedByInsert()
					&& !forceIdentifierBinding ) {
				assert entityPersister.getInsertDelegate() != null;
				final var generator = (OnExecutionGenerator) entityPersister.getGenerator();
				if ( generator.referenceColumnsInSql( dialect ) ) {
					final String[] columnValues = generator.getReferencedColumnValues( dialect );
					if ( columnValues != null ) {
						// Handle both single-column and composite key scenarios
						final var keyColumns = tableDescriptor.keyDescriptor().columns();
						assert columnValues.length == keyColumns.size()
								: "Mismatch between referenced column values and key columns: "
								+ columnValues.length + " vs " + keyColumns.size();

						// For composite keys, each column may have its own generation strategy
						for ( int i = 0; i < columnValues.length; i++ ) {
							if ( columnValues[i] != null ) {
								builder.addColumnAssignment( keyColumns.get( i ), columnValues[i] );
							}
							else {
								// No special value reference - use parameter binding
								builder.addColumnAssignment( keyColumns.get( i ) );
							}
						}
					}
					else {
						// No referenced column values - all key columns use parameter binding
						for (var keyColumn : tableDescriptor.keyDescriptor().columns()) {
							builder.addColumnAssignment( keyColumn );
						}
					}
				}
				else {
					// Generator doesn't reference columns in SQL - use parameter binding
					for (var keyColumn : tableDescriptor.keyDescriptor().columns()) {
						builder.addColumnAssignment( keyColumn );
					}
				}
			}
			else {
				for (var keyColumn : tableDescriptor.keyDescriptor().columns()) {
					builder.addColumnAssignment( keyColumn );
				}
			}
		} );
	}

	protected Map<String, TableInsert> generateDynamicInsertOperations(
			boolean[] insertable,
			Object object,
			SharedSessionContractImplementor session,
			boolean forceIdentifierBinding) {
		final Map<String, TableInsertBuilder> operationBuilders = CollectionHelper.linkedMapOfSize( entityPersister.getTableDescriptors().length );
		entityPersister.forEachMutableTableDescriptor( (tableDescriptor) -> {
			operationBuilders.put(
					tableDescriptor.name(),
					createTableInsertBuilder(tableDescriptor, false)
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
		operationBuilders.forEach(  (name, operationBuilder) -> {
			operations.put( name, operationBuilder.buildMutation() );
		} );
		return operations;
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

	private EntityInsertBindPlan createInsertBindPlan(
			EntityTableDescriptor tableDescriptor,
			Object entity,
			Object identifier,
			Object[] state,
			boolean[] insertable,
			AbstractEntityInsertAction action,
			GeneratedValuesCollector generatedValuesCollector) {
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
				generatedValuesCollector
		);
	}

}
