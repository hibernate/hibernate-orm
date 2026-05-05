/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;

import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;

import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.spi.bind.GeneratedValuesCollector;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.action.queue.spi.meta.TableDescriptorAsTableMapping;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.Generator;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableInsert;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;

import static org.hibernate.generator.EventType.UPDATE;

/// Graph mutation plan contributor for single-table temporal entity mutations.
///
/// @author Steve Ebersole
public class TemporalEntityMutationPlanContributor implements EntityMutationPlanContributor {
	private final EntityPersister entityPersister;
	private final SessionFactoryImplementor sessionFactory;
	private final TemporalMapping temporalMapping;
	private final EntityInsertMutationPlanner insertMutationPlanner;
	private final MutationOperation staticEndOperation;

	public TemporalEntityMutationPlanContributor(
			EntityPersister entityPersister,
			SessionFactoryImplementor sessionFactory) {
		this.entityPersister = entityPersister;
		this.sessionFactory = sessionFactory;
		this.temporalMapping = entityPersister.getTemporalMapping();
		this.insertMutationPlanner = new EntityInsertMutationPlanner( entityPersister, sessionFactory );
		this.staticEndOperation = buildEndOperation( null, null, effectiveOptimisticLockStyle( null, null ), null );
	}

	@Override
	public boolean contributeReplacementUpdate(
			UpdateContext context,
			Consumer<FlushOperation> operationConsumer) {
		if ( entityPersister.excludedFromTemporalVersioning(
				context.action().getDirtyFields(),
				context.action().hasDirtyCollection() ) ) {
			return false;
		}

		preUpdateInMemoryValueGeneration( context.entity(), context.state(), context.session() );

		final OptimisticLockStyle effectiveOptLockStyle = effectiveOptimisticLockStyle(
				context.previousVersion(),
				context.previousState()
		);
		final MutationOperation endOperation = needsDynamicEndOperation(
				context.rowId(),
				context.previousState(),
				effectiveOptLockStyle
		)
				? buildEndOperation(
						context.rowId(),
						context.previousState(),
						effectiveOptLockStyle,
						context.session()
				)
				: staticEndOperation;

		operationConsumer.accept( createEndOperation(
				context.ordinalBase(),
				"EntityUpdateAction(" + entityPersister.getEntityName() + ")",
				context.identifier(),
				context.rowId(),
				context.previousVersion(),
				context.previousState(),
				effectiveOptLockStyle,
				endOperation
		) );

		contributeReplacementInsert( context, operationConsumer );
		return true;
	}

	@Override
	public boolean contributeReplacementDelete(
			DeleteContext context,
			Consumer<FlushOperation> operationConsumer) {
		final var entityEntry = context.session().getPersistenceContextInternal().getEntry( context.action().getInstance() );
		final Object[] loadedState = entityEntry == null ? context.state() : entityEntry.getLoadedState();
		final Object rowId = entityEntry == null ? null : entityEntry.getRowId();
		final OptimisticLockStyle effectiveOptLockStyle = effectiveOptimisticLockStyle( context.version(), loadedState );

		final MutationOperation operation = needsDynamicEndOperation( rowId, loadedState, effectiveOptLockStyle )
				? buildEndOperation( rowId, loadedState, effectiveOptLockStyle, context.session() )
				: staticEndOperation;

		final FlushOperation flushOperation = createEndOperation(
				context.ordinalBase(),
				"EntityDeleteAction(" + entityPersister.getEntityName() + ")",
				context.identifier(),
				rowId,
				context.version(),
				loadedState,
				effectiveOptLockStyle,
				operation
		);
		flushOperation.setPreExecutionCallback( context.postDeleteHandling().getPreDeleteHandling() );
		flushOperation.setPostExecutionCallback( context.postDeleteHandling() );
		operationConsumer.accept( flushOperation );
		return true;
	}

	private FlushOperation createEndOperation(
			int ordinalBase,
			String description,
			Object identifier,
			Object rowId,
			Object version,
			Object[] loadedState,
			OptimisticLockStyle effectiveOptLockStyle,
			MutationOperation operation) {
		return new FlushOperation(
				entityPersister.getIdentifierTableDescriptor(),
				MutationKind.UPDATE,
				operation,
				new EntityTemporalEndBindPlan(
						entityPersister.getIdentifierTableDescriptor(),
						entityPersister,
						temporalMapping,
						identifier,
						rowId,
						version,
						loadedState,
						effectiveOptLockStyle
				),
				ordinalBase * 1_000,
				description
		);
	}

	private void contributeReplacementInsert(
			UpdateContext context,
			Consumer<FlushOperation> operationConsumer) {
		final boolean hasStateDependentInsertGenerator = insertMutationPlanner.preInsertInMemoryValueGeneration(
				context.state(),
				context.entity(),
				context.session()
		);
		final boolean[] effectiveInsertability = insertMutationPlanner.resolveInsertability( context.state() );
		final Map<String, TableInsert> insertGroup = insertMutationPlanner.resolveInsertOperations(
				effectiveInsertability,
				context.entity(),
				context.identifier(),
				hasStateDependentInsertGenerator,
				context.session()
		);

		final InsertValuesAnalysis insertValuesAnalysis = new InsertValuesAnalysis( entityPersister, context.state() );
		final GeneratedValuesCollector generatedValuesCollector = GeneratedValuesCollector.forInsert(
				entityPersister,
				sessionFactory
		);
		final PostUpdateHandling postUpdateHandling = new PostUpdateHandling(
				context.action(),
				context.cacheUpdate(),
				context.previousVersion(),
				generatedValuesCollector,
				context.entityEntry()
		);

		int localOrd = 1;
		FlushOperation previousOperation = null;
		for ( Map.Entry<String, TableInsert> entry : insertGroup.entrySet() ) {
			final var operation = entry.getValue().createMutationOperation( insertValuesAnalysis, sessionFactory );
			final var tableMapping = (TableDescriptorAsTableMapping) operation.getTableDetails();
			final var tableDescriptor = (EntityTableDescriptor) tableMapping.descriptor();

			if ( !insertValuesAnalysis.include( tableDescriptor ) ) {
				continue;
			}

			final BindPlan bindPlan = insertMutationPlanner.createInsertBindPlan(
					tableDescriptor,
					context.entity(),
					context.identifier(),
					context.state(),
					effectiveInsertability,
					null,
					generatedValuesCollector,
					context.decompositionContext()
			);

			final FlushOperation op = new FlushOperation(
					tableDescriptor,
					MutationKind.INSERT,
					operation,
					bindPlan,
					context.ordinalBase() * 1_000 + (localOrd++),
					"EntityUpdateAction(" + entityPersister.getEntityName() + ")"
			);

			if ( previousOperation != null ) {
				operationConsumer.accept( previousOperation );
			}
			previousOperation = op;
		}

		if ( previousOperation != null ) {
			previousOperation.setPostExecutionCallback( postUpdateHandling );
			operationConsumer.accept( previousOperation );
		}
	}

	private int[] preUpdateInMemoryValueGeneration(
			Object object,
			Object[] newValues,
			SharedSessionContractImplementor session) {
		if ( !entityPersister.hasPreUpdateGeneratedProperties() ) {
			return org.hibernate.internal.util.collections.ArrayHelper.EMPTY_INT_ARRAY;
		}

		final var generators = entityPersister.getGenerators();
		if ( generators.length != 0 ) {
			final int[] fieldsPreUpdateNeeded = new int[generators.length];
			int count = 0;
			for ( int i = 0; i < generators.length; i++ ) {
				final Generator generator = generators[i];
				if ( generator != null
						&& generator.generatesOnUpdate()
						&& generator.generatedBeforeExecution( object, session ) ) {
					newValues[i] = ( (BeforeExecutionGenerator) generator ).generate( session, object, newValues[i], UPDATE );
					entityPersister.setValue( object, i, newValues[i] );
					fieldsPreUpdateNeeded[count++] = i;
				}
			}

			if ( count > 0 ) {
				return org.hibernate.internal.util.collections.ArrayHelper.trim( fieldsPreUpdateNeeded, count );
			}
		}

		return org.hibernate.internal.util.collections.ArrayHelper.EMPTY_INT_ARRAY;
	}

	private boolean needsDynamicEndOperation(
			Object rowId,
			Object[] loadedState,
			OptimisticLockStyle effectiveOptLockStyle) {
		return rowId != null
			|| effectiveOptLockStyle.isAllOrDirty()
			|| entityPersister.hasPartitionedSelectionMapping() && loadedState != null;
	}

	private OptimisticLockStyle effectiveOptimisticLockStyle(Object version, Object[] loadedState) {
		final OptimisticLockStyle optimisticLockStyle = entityPersister.optimisticLockStyle();
		if ( optimisticLockStyle.isVersion() ) {
			return version == null || entityPersister.getVersionMapping() == null
					? OptimisticLockStyle.NONE
					: optimisticLockStyle;
		}
		return optimisticLockStyle.isAllOrDirty() && loadedState == null
				? OptimisticLockStyle.NONE
				: optimisticLockStyle;
	}

	private MutationOperation buildEndOperation(
			Object rowId,
			Object[] loadedState,
			OptimisticLockStyle effectiveOptLockStyle,
			SharedSessionContractImplementor session) {
		final EntityTableDescriptor tableDescriptor = entityPersister.getIdentifierTableDescriptor();
		final var tableMapping = new TableDescriptorAsTableMapping(
				tableDescriptor,
				tableDescriptor.getRelativePosition(),
				true,
				false
		);
		final var updateBuilder = new TableUpdateBuilderStandard<>(
				entityPersister,
				new MutatingTableReference( tableMapping ),
				sessionFactory
		);

		addKeyRestriction( updateBuilder, tableDescriptor, rowId );
		addTemporalEnding( updateBuilder );
		addVersionRestriction( updateBuilder, tableDescriptor );
		addNonVersionOptimisticLockRestrictions( updateBuilder, tableDescriptor, loadedState, effectiveOptLockStyle, session );
		addPartitionedSelectionRestrictions( updateBuilder, tableDescriptor, loadedState );

		return updateBuilder.buildMutation().createMutationOperation( null, sessionFactory );
	}

	private void addKeyRestriction(
			TableUpdateBuilderStandard<MutationOperation> updateBuilder,
			EntityTableDescriptor tableDescriptor,
			Object rowId) {
		if ( rowId != null && entityPersister.getRowIdMapping() != null ) {
			updateBuilder.addKeyRestrictionLeniently( entityPersister.getRowIdMapping() );
		}
		else {
			updateBuilder.addKeyRestrictions( tableDescriptor.keyDescriptor() );
		}
	}

	private void addTemporalEnding(TableUpdateBuilderStandard<MutationOperation> updateBuilder) {
		final var endingColumn = new ColumnReference(
				updateBuilder.getMutatingTable(),
				temporalMapping.getEndingColumnMapping()
		);
		updateBuilder.addValueColumn( temporalMapping.createEndingValueBinding( endingColumn ) );
		updateBuilder.addNonKeyRestriction( temporalMapping.createNullEndingValueBinding( endingColumn ) );
	}

	private void addVersionRestriction(
			TableUpdateBuilderStandard<MutationOperation> updateBuilder,
			EntityTableDescriptor tableDescriptor) {
		final var versionMapping = entityPersister.getVersionMapping();
		if ( versionMapping != null
				&& entityPersister.optimisticLockStyle().isVersion()
				&& tableDescriptor.name().equals( versionMapping.getContainingTableExpression() ) ) {
			updateBuilder.addOptimisticLockRestriction( versionMapping );
		}
	}

	private void addPartitionedSelectionRestrictions(
			TableUpdateBuilderStandard<MutationOperation> updateBuilder,
			EntityTableDescriptor tableDescriptor,
			Object[] loadedState) {
		if ( loadedState == null || !entityPersister.hasPartitionedSelectionMapping() ) {
			return;
		}
		for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
			final var attribute = tableDescriptor.attributes().get( i );
			attribute.forEachSelectable( (selectionIndex, selectableMapping) -> {
				if ( selectableMapping.isPartitioned() ) {
					updateBuilder.addKeyRestrictionLeniently( selectableMapping );
				}
			} );
		}
	}

	private void addNonVersionOptimisticLockRestrictions(
			TableUpdateBuilderStandard<MutationOperation> updateBuilder,
			EntityTableDescriptor tableDescriptor,
			Object[] loadedState,
			OptimisticLockStyle effectiveOptLockStyle,
			SharedSessionContractImplementor session) {
		if ( loadedState == null || !effectiveOptLockStyle.isAllOrDirty() ) {
			return;
		}

		final boolean[] versionability = entityPersister.getPropertyVersionability();
		for ( int i = 0; i < tableDescriptor.attributes().size(); i++ ) {
			final var attribute = tableDescriptor.attributes().get( i );
			if ( attribute.isPluralAttributeMapping() || !versionability[attribute.getStateArrayPosition()] ) {
				continue;
			}
			attribute.breakDownJdbcValues(
					loadedState[attribute.getStateArrayPosition()],
					(valueIndex, jdbcValue, jdbcValueMapping) -> {
						if ( !jdbcValueMapping.isFormula() ) {
							if ( jdbcValue == null ) {
								updateBuilder.addNullOptimisticLockRestriction( jdbcValueMapping );
							}
							else {
								updateBuilder.addOptimisticLockRestriction( jdbcValueMapping );
							}
						}
					},
					session
			);
		}
	}
}
