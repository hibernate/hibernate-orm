/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.bind.EntityInsertBindPlan;
import org.hibernate.action.queue.bind.GeneratedValuesCollector;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.ast.TableInsert;
import org.hibernate.action.queue.mutation.ast.builder.GraphTableInsertBuilder;
import org.hibernate.action.queue.mutation.ast.builder.GraphTableInsertBuilderStandard;
import org.hibernate.action.queue.op.PlannedOperation;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


/// [Decomposer][org.hibernate.action.queue.graph.MutationDecomposer] for entity insert operations.
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
			Consumer<PostExecutionCallback> postExecutionCallbackRegistry,
			SharedSessionContractImplementor session) {
		final boolean vetoed = preInsert( action, session );
		if ( vetoed ) {
			return List.of();
		}

		// Nullify transient references before decomposing - this ensures bidirectional
		// associations are handled correctly and nullability checks are performed
		action.nullifyTransientReferencesIfNotAlready();

		final Object entity = action.getInstance();
		final Object identifier = action.getId();
		final Object[] state = action.getState();

		var insertable = entityPersister.getPropertyInsertability();
		var valuesAnalysis = new SimpleInsertValuesAnalysis( entityPersister, state );
		var effectiveGroup = chooseEffectiveInsertGroup( insertable, entity, identifier, session );

		final var generatedValuesCollector = GeneratedValuesCollector.forInsert( entityPersister );
		final PostInsertHandling postInsertHandling = new PostInsertHandling( action, generatedValuesCollector );
		postExecutionCallbackRegistry.accept( postInsertHandling );

		// Compute whether this entity insert needs identity pre-phase
		final boolean needsIdPrePhase = Helper.needsIdentityPrePhase(entityPersister, identifier);

		final List<PlannedOperation> operations = CollectionHelper.arrayList( effectiveGroup.size() );
		int localOrd = 0;
		int i = 0;
		for ( Map.Entry<String, TableInsert> entry : effectiveGroup.entrySet() ) {
			var operation = entry.getValue().createMutationOperation();
			var tableDescriptor = (EntityTableDescriptor) operation.getTableDescriptor();

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

		return operations;
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

	private Map<String, TableInsert> chooseEffectiveInsertGroup(
			boolean[] insertable,
			Object entity,
			Object id,
			SharedSessionContractImplementor session) {
		final boolean forceIdentifierBinding = entityPersister.getGenerator().generatedOnExecution() && id != null;
		return entityPersister.isDynamicInsert() || forceIdentifierBinding
				? generateDynamicInsertOperations( insertable, entity, session, forceIdentifierBinding )
				: staticInsertOperations;
	}

	private Map<String, TableInsert> generateStaticOperations() {
		final Map<String, GraphTableInsertBuilder> staticOperationBuilders = new HashMap<>();
		entityPersister.forEachMutableTableDescriptor( (tableDescriptor) -> {
			staticOperationBuilders.put(
					tableDescriptor.name(),
					createGraphTableInsertBuilder(tableDescriptor, false)
			);
		} );

		applyGraphTableInsertDetails(
				staticOperationBuilders,
				entityPersister.getPropertyInsertability(),
				null,
				null,
				false
		);

		final Map<String, TableInsert> staticOperations = new HashMap<>();
		staticOperationBuilders.forEach(  (name, operationBuilder) -> {
			staticOperations.put( name, operationBuilder.buildMutation() );
		} );
		return Collections.unmodifiableMap( staticOperations );
	}

	private GraphTableInsertBuilder createGraphTableInsertBuilder(
			TableDescriptor tableDescriptor,
			boolean forceIdentifierBinding) {
		final var delegate = entityPersister.getInsertDelegate();
		// TODO: Handle custom insert delegates for graph mutations
		// For now, always use standard builder
		return new GraphTableInsertBuilderStandard(
				entityPersister,
				tableDescriptor,
				sessionFactory
		);
	}

	private void applyGraphTableInsertDetails(
			Map<String, GraphTableInsertBuilder> builders,
			boolean[] attributeInclusions,
			Object object,
			SharedSessionContractImplementor session,
			boolean forceIdentifierBinding) {
		final Dialect dialect = sessionFactory.getJdbcServices().getDialect();

		builders.forEach( (name, builder) -> {
			final var tableDescriptor = (EntityTableDescriptor) builder.getTableDescriptor();
			// Skip inverse tables
			if (tableDescriptor.isInverse()) {
				return;
			}

			for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
				var attribute = tableDescriptor.attributes().get(i);
				if ( attributeInclusions[attribute.getStateArrayPosition()] ) {
					tableDescriptor.forEachAttributeColumn( attribute, builder::addValueColumn );
				}
				else {
					final var generator = attribute.getGenerator();
					if ( isValueGenerated( generator ) ) {
						if ( session != null && generator.generatedBeforeExecution( object, session ) ) {
							attributeInclusions[attribute.getStateArrayPosition()] = true;
							tableDescriptor.forEachAttributeColumn( attribute, builder::addValueColumn );
						}
						else if ( isValueGenerationInSql( generator, dialect ) ) {
							handleValueGeneration( attribute, builder, (OnExecutionGenerator) generator );
						}
					}
				}
			}
		} );

		entityPersister.addDiscriminatorToInsertGroup( (tableName) -> builders.get( tableName ) );
		entityPersister.addSoftDeleteToInsertGroup( (tableName) -> builders.get( tableName ) );

		// add the keys
		builders.forEach( (name, builder) -> {
			if ( ( (EntityTableDescriptor) builder.getTableDescriptor() ).isIdentifierTable()
				&& entityPersister.isIdentifierAssignedByInsert()
				&& !forceIdentifierBinding ) {
				assert entityPersister.getInsertDelegate() != null;
				final var generator = (OnExecutionGenerator) entityPersister.getGenerator();
				if ( generator.referenceColumnsInSql( dialect ) ) {
					final String[] columnValues = generator.getReferencedColumnValues( dialect );
					if ( columnValues != null ) {
						assert columnValues.length == 1;
						assert builder.getTableDescriptor().keyDescriptor().columns().size() == 1;
						builder.addKeyColumn( columnValues[0], builder.getTableDescriptor().keyDescriptor().columns().get( 0 ) );
					}
				}
			}
			else {
				for (var keyColumn : builder.getTableDescriptor().keyDescriptor().columns()) {
					builder.addKeyColumn(keyColumn);
				}
			}
		} );
	}

	protected Map<String, TableInsert> generateDynamicInsertOperations(
			boolean[] insertable,
			Object object,
			SharedSessionContractImplementor session,
			boolean forceIdentifierBinding) {
		final Map<String, GraphTableInsertBuilder> operationBuilders = new HashMap<>();
		entityPersister.forEachMutableTableDescriptor( (tableDescriptor) -> {
			operationBuilders.put(
					tableDescriptor.name(),
					createGraphTableInsertBuilder(tableDescriptor, false)
			);
		} );

		applyGraphTableInsertDetails(
				operationBuilders,
				insertable,
				object,
				session,
				forceIdentifierBinding
		);

		final Map<String, TableInsert> operations = new HashMap<>();
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
