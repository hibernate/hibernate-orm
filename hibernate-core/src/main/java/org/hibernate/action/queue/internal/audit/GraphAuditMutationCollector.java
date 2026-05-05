/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.audit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.Session;
import org.hibernate.action.queue.internal.exec.PlanStepExecutorFactory;
import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.audit.EntityTrackingChangesetListener;
import org.hibernate.audit.ModificationType;
import org.hibernate.audit.spi.AuditChangeSet;
import org.hibernate.audit.spi.ChangelogSupplier;
import org.hibernate.audit.spi.ChangelogSupplier.ChangesetContext;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.mutation.EntityAuditSupport;
import org.hibernate.persister.collection.mutation.CollectionAuditSupport;

import static org.hibernate.action.queue.internal.audit.GraphAuditMutationPlans.CollectionAuditInsertPlan;
import static org.hibernate.action.queue.internal.audit.GraphAuditMutationPlans.CollectionAuditRowChange;
import static org.hibernate.action.queue.internal.audit.GraphAuditMutationPlans.CollectionTransactionEndPlan;
import static org.hibernate.action.queue.internal.audit.GraphAuditMutationPlans.EntityAuditInsertPlan;
import static org.hibernate.action.queue.internal.audit.GraphAuditMutationPlans.EntityTransactionEndPlan;

/// Transaction-scoped audit mutation collector for the graph action queue.
///
/// @author Steve Ebersole
public class GraphAuditMutationCollector {
	private final AuditChangeSet<EntityAuditSupport, CollectionAuditSupport> changeSet = new AuditChangeSet<>();
	private Object changelog;
	private @Nullable Session changesetSession;

	public void entityChanged(
			EntityKey entityKey,
			Object entity,
			Object[] values,
			ModificationType modificationType,
			EntityAuditSupport mutationSupport) {
		changeSet.addEntityChange( entityKey, entity, values, modificationType, mutationSupport );
	}

	public void collectionChanged(
			CollectionPersister collectionPersister,
			PersistentCollection<?> collection,
			Object ownerId,
			Object originalSnapshot,
			CollectionAuditSupport mutationSupport) {
		changeSet.addCollectionChange( collectionPersister, collection, ownerId, originalSnapshot, mutationSupport );
	}

	public boolean hasWork() {
		return !changeSet.isEmpty();
	}

	public void executeAuditMutations(SharedSessionContractImplementor session) {
		if ( changeSet.isEmpty() ) {
			return;
		}

		final var changesetContext = resolveChangesetContext( session );
		final Object changesetId = changesetContext == null
				? session.getCurrentChangesetIdentifier()
				: changesetContext.changesetId();
		if ( changesetContext != null ) {
			changelog = changesetContext.changelog();
			changesetSession = changesetContext.changesetSession();
		}

		final List<AuditChangeSet.EntityChange<EntityAuditSupport>> entityChanges = changeSet.entityChanges();
		final List<AuditChangeSet.CollectionChange<CollectionAuditSupport>> collectionChanges = changeSet.collectionChanges();
		final List<FlushOperation> operations = new ArrayList<>( entityChanges.size() * 2 + collectionChanges.size() * 4 );
		createEntityTransactionEndOperations( entityChanges, changesetId, operations );
		createEntityAuditInsertOperations( entityChanges, changesetId, session, operations );
		createCollectionAuditOperations( collectionChanges, changesetId, operations );

		try {
			if ( !operations.isEmpty() ) {
				final var executor = PlanStepExecutorFactory.create( session );
				executor.execute( operations, null, null );
				executor.finishUp();
			}
			executeChangesetCallbacks( entityChanges, session );
		}
		finally {
			clear();
		}
	}

	public void clear() {
		changeSet.clear();
		changelog = null;
		changesetSession = null;
	}

	private @Nullable ChangesetContext<?> resolveChangesetContext(SharedSessionContractImplementor session) {
		final var context = session.getCurrentChangesetContext();
		return context instanceof ChangesetContext<?> changesetContext
				? changesetContext
				: null;
	}

	private void executeChangesetCallbacks(
			List<AuditChangeSet.EntityChange<EntityAuditSupport>> entityChanges,
			SharedSessionContractImplementor session) {
		if ( changelog == null || entityChanges.isEmpty() ) {
			return;
		}

		final var supplier = ChangelogSupplier.resolve( session.getFactory().getServiceRegistry() );
		final var listener = resolveTrackingListener( supplier );
		if ( listener != null ) {
			for ( var change : entityChanges ) {
				final var entityKey = change.entityKey();
				listener.entityChanged(
						entityKey.getPersister().getMappedClass(),
						entityKey.getIdentifier(),
						change.modificationType(),
						changelog
				);
			}
		}
		populateModifiedEntityNames( supplier, entityChanges, session );
	}

	private void populateModifiedEntityNames(
			@Nullable ChangelogSupplier<?> supplier,
			List<AuditChangeSet.EntityChange<EntityAuditSupport>> entityChanges,
			SharedSessionContractImplementor session) {
		if ( supplier == null || supplier.getModifiedEntitiesProperty() == null ) {
			return;
		}

		final var persister = session.getEntityPersister(
				supplier.getChangelogClass().getName(),
				changelog
		);
		final var attr = persister.findAttributeMapping( supplier.getModifiedEntitiesProperty() );
		//noinspection unchecked
		var entityNames = (Set<String>) persister.getValue( changelog, attr.getStateArrayPosition() );
		if ( entityNames == null ) {
			entityNames = new HashSet<>();
			persister.setValue( changelog, attr.getStateArrayPosition(), entityNames );
		}
		for ( var change : entityChanges ) {
			entityNames.add( change.entityKey().getEntityName() );
		}
		if ( changesetSession != null ) {
			changesetSession.flush();
		}
	}

	private static @Nullable EntityTrackingChangesetListener resolveTrackingListener(
			@Nullable ChangelogSupplier<?> supplier) {
		if ( supplier != null && supplier.getListener() instanceof EntityTrackingChangesetListener listener ) {
			return listener;
		}
		return null;
	}

	private void createEntityTransactionEndOperations(
			List<AuditChangeSet.EntityChange<EntityAuditSupport>> changes,
			Object changesetId,
			List<FlushOperation> operations) {
		int ordinal = 0;
		for ( var change : changes ) {
			final EntityAuditSupport mutationSupport = change.entityAuditHandler();
			for ( var plan : mutationSupport.resolveTransactionEndUpdatePlans() ) {
				final var graphPlan = new EntityTransactionEndPlan( mutationSupport, plan );
				final var operation = plan.operation();
				operations.add( new FlushOperation(
						operation.tableDescriptor(),
						MutationKind.UPDATE,
						operation.operation(),
						graphPlan.createBindPlan( change, changesetId ),
						ordinal++,
						graphPlan.describe( change )
				) );
			}
		}
	}

	private void createEntityAuditInsertOperations(
			List<AuditChangeSet.EntityChange<EntityAuditSupport>> changes,
			Object changesetId,
			SharedSessionContractImplementor session,
			List<FlushOperation> operations) {
		int ordinal = operations.size();
		for ( var change : changes ) {
			final EntityAuditSupport mutationSupport = change.entityAuditHandler();
			final boolean[] propertyInclusions = mutationSupport.resolvePropertyInclusions(
					change.entity(),
					change.values(),
					session
			);
			for ( var plan :
					mutationSupport.resolveAuditInsertPlans( propertyInclusions, change.entity(), session ) ) {
				final var graphPlan = new EntityAuditInsertPlan( mutationSupport, plan );
				final var operation = plan.operation();
				operations.add( new FlushOperation(
						operation.tableDescriptor(),
						MutationKind.INSERT,
						operation.operation(),
						graphPlan.createBindPlan( change, changesetId ),
						ordinal++,
						graphPlan.describe( change )
				) );
			}
		}
	}

	private void createCollectionAuditOperations(
			List<AuditChangeSet.CollectionChange<CollectionAuditSupport>> collectionChanges,
			Object changesetId,
			List<FlushOperation> operations) {
		int ordinal = operations.size();
		for ( var collectionChange : collectionChanges ) {
			final var mutationSupport = collectionChange.collectionAuditHandler();
			final var changes = mutationSupport.resolveChanges(
					collectionChange.collection(),
					collectionChange.originalSnapshot()
			);
			final var transactionEndPlan = mutationSupport.resolveTransactionEndUpdatePlan();
			if ( transactionEndPlan != null ) {
				final var graphPlan = new CollectionTransactionEndPlan( mutationSupport, transactionEndPlan );
				final var operation = transactionEndPlan.operation();
				for ( var change : changes ) {
					final var rowChange = new CollectionAuditRowChange(
							collectionChange.collection(),
							collectionChange.ownerId(),
							change
					);
					operations.add( new FlushOperation(
							operation.tableDescriptor(),
							MutationKind.UPDATE,
							operation.operation(),
							graphPlan.createBindPlan( rowChange, changesetId ),
							ordinal++,
							graphPlan.describe()
					) );
				}
			}
			final var insertPlan = mutationSupport.resolveAuditInsertPlan();
			if ( insertPlan != null ) {
				final var graphPlan = new CollectionAuditInsertPlan( mutationSupport, insertPlan );
				final var operation = insertPlan.operation();
				for ( var change : changes ) {
					final var rowChange = new CollectionAuditRowChange(
							collectionChange.collection(),
							collectionChange.ownerId(),
							change
					);
					operations.add( new FlushOperation(
							operation.tableDescriptor(),
							MutationKind.INSERT,
							operation.operation(),
							graphPlan.createBindPlan( rowChange, changesetId ),
							ordinal++,
							graphPlan.describe()
					) );
				}
			}
		}
	}
}
