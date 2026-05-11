/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;

import org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor;
import org.hibernate.action.queue.spi.decompose.entity.InsertCacheHandling;

import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.spi.bind.GeneratedValuesCollector;
import org.hibernate.action.queue.spi.bind.PostExecutionCallback;
import org.hibernate.action.queue.spi.decompose.DecompositionContext;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.internal.Nullability;
import org.hibernate.sql.model.ast.TableInsert;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.action.queue.internal.support.Helper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.persister.entity.EntityPersister;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


/// [Decomposer][org.hibernate.action.queue.spi.decompose.entity.EntityActionDecomposer] for entity insert operations.
///
/// Converts an [AbstractEntityInsertAction] into a group of [FlushOperation] to be performed.
///
/// @apiNote Insert decomposition does not currently use an [EntityMutationPlanContributor].
/// State-management-specific graph mutation plans are contributed for logical
/// update/delete actions, where the logical action may need to be represented
/// by a different physical mutation shape.
///
/// @author Steve Ebersole
public class InsertDecomposer extends AbstractDecomposer<AbstractEntityInsertAction> {
	private final EntityInsertMutationPlanner insertMutationPlanner;
	private final EntityMutationPlanContributor mutationPlanContributor;

	public InsertDecomposer(EntityPersister entityPersister, SessionFactoryImplementor sessionFactory) {
		this( entityPersister, sessionFactory, EntityMutationPlanContributor.STANDARD );
	}

	public InsertDecomposer(
			EntityPersister entityPersister,
			SessionFactoryImplementor sessionFactory,
			EntityMutationPlanContributor mutationPlanContributor) {
		super( entityPersister, sessionFactory );

		this.insertMutationPlanner = new EntityInsertMutationPlanner( entityPersister, sessionFactory );
		this.mutationPlanContributor = mutationPlanContributor;
	}

	/// Static set of table mutations used to perform the entity creation.
	public Map<String, TableInsert> getStaticInsertOperations() {
		return insertMutationPlanner.getStaticInsertOperations();
	}

	public boolean[] resolveInsertability(Object[] state) {
		return insertMutationPlanner.resolveInsertability( state );
	}

	public Map<String, TableInsert> resolveInsertOperations(
			boolean[] effectiveInsertability,
			Object entity,
			Object identifier,
			boolean hasStateDependentGenerator,
			SharedSessionContractImplementor session) {
		return insertMutationPlanner.resolveInsertOperations(
				effectiveInsertability,
				entity,
				identifier,
				hasStateDependentGenerator,
				session
		);
	}

	@Override
	public void decompose(
			AbstractEntityInsertAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Consumer<FlushOperation> operationConsumer) {
		final Object entity = action.getInstance();
		if ( decompositionContext != null && decompositionContext.isBeingDeletedInCurrentFlush( entity ) ) {
			return;
		}

		final boolean vetoed = preInsert( action, session );
		if ( vetoed ) {
			return;
		}

		// Nullify transient references before decomposing - this ensures bidirectional
		// associations are handled correctly and nullability checks are performed
		nullifyTransientReferencesIfNotAlready( action, session, decompositionContext );

		final Object identifier = action.getId();
		final Object[] state = action.getState();

		// apply any pre-insert in-memory value generation
		final boolean hasStateDependentGenerator = insertMutationPlanner.preInsertInMemoryValueGeneration(
				state,
				entity,
				session
		);

		var insertable = entityPersister.getPropertyInsertability();
		var valuesAnalysis = new InsertValuesAnalysis( entityPersister, state );
		final boolean[] effectiveInsertability = entityPersister.isDynamicInsert()
				? insertMutationPlanner.resolveInsertability( state )
				: insertable;
		var effectiveGroup = insertMutationPlanner.resolveInsertOperations(
				effectiveInsertability,
				entity,
				identifier,
				hasStateDependentGenerator,
				session
		);

		final var generatedValuesCollector = GeneratedValuesCollector.forInsert( entityPersister, sessionFactory );
		if ( generatedValuesCollector != null && decompositionContext != null ) {
			generatedValuesCollector.setIdentifierHandle( decompositionContext.getGeneratedIdentifierHandle( entity ) );
		}
		final InsertCacheHandling.CacheInsert cacheInsert = new InsertCacheHandling.CacheInsert();
		registerAfterTransactionCompletion( action, cacheInsert, session );
		final PostInsertHandling postInsertHandling = new PostInsertHandling(
				action,
				generatedValuesCollector,
				cacheInsert
		);

		// Compute whether this entity insert needs identity pre-phase
		final boolean needsIdPrePhase = Helper.needsIdentityPrePhase(entityPersister, identifier);

		int localOrd = 0;
		FlushOperation previousOperation = null;
		for ( var tableDescriptor : entityPersister.getTableDescriptors() ) {
			if ( tableDescriptor.isInverse() ) {
				continue;
			}
			final var tableInsert = effectiveGroup.get( tableDescriptor.name() );
			var operation = tableInsert.createMutationOperation( valuesAnalysis, sessionFactory );

			if ( !valuesAnalysis.include( tableDescriptor ) ) {
				continue;
			}

			final BindPlan bindPlan = insertMutationPlanner.createInsertBindPlan(
					tableDescriptor,
					entity,
					identifier,
					state,
					effectiveInsertability,
					action,
					generatedValuesCollector,
					decompositionContext
			);

			final FlushOperation op = new FlushOperation(
					tableDescriptor,
					MutationKind.INSERT,
					operation,
					bindPlan,
					ordinalBase * 1_000 + (localOrd++),
					"EntityInsertAction(" + entityPersister.getEntityName() + ")",
					needsIdPrePhase
			);

			if ( previousOperation != null ) {
				operationConsumer.accept( previousOperation );
			}
			previousOperation = op;
		}

		final List<FlushOperation> additionalOperations = new ArrayList<>();
		mutationPlanContributor.contributeAdditionalInsert(
				new EntityMutationPlanContributor.InsertContext(
						entityPersister,
						action,
						ordinalBase,
						session,
						decompositionContext,
						entity,
						identifier,
						state,
						cacheInsert
				),
				additionalOperations::add
		);

		emitTailOperations( previousOperation, additionalOperations, postInsertHandling, operationConsumer );
	}

	private void emitTailOperations(
			FlushOperation previousOperation,
			List<FlushOperation> additionalOperations,
			PostExecutionCallback postExecutionCallback,
			Consumer<FlushOperation> operationConsumer) {
		if ( additionalOperations.isEmpty() ) {
			if ( previousOperation != null ) {
				previousOperation.setPostExecutionCallback( postExecutionCallback );
				operationConsumer.accept( previousOperation );
			}
			return;
		}

		if ( previousOperation != null ) {
			operationConsumer.accept( previousOperation );
		}
		for ( int i = 0; i < additionalOperations.size() - 1; i++ ) {
			operationConsumer.accept( additionalOperations.get( i ) );
		}
		final FlushOperation lastOperation = additionalOperations.get( additionalOperations.size() - 1 );
		lastOperation.setPostExecutionCallback( postExecutionCallback );
		operationConsumer.accept( lastOperation );
	}

	private void registerAfterTransactionCompletion(
			AbstractEntityInsertAction action,
			InsertCacheHandling.CacheInsert cacheInsert,
			SharedSessionContractImplementor session) {
		final var callback = new InsertAfterTransactionCompletionHandling( action, cacheInsert );
		if ( callback.isNeeded( session ) ) {
			session.getTransactionCompletionCallbacks().registerCallback( callback );
		}
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

	public boolean preInsertInMemoryValueGeneration(Object[] values, Object entity, SharedSessionContractImplementor session) {
		return insertMutationPlanner.preInsertInMemoryValueGeneration( values, entity, session );
	}

}
