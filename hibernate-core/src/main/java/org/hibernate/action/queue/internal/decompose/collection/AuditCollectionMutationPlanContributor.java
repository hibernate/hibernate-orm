/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.collection;

import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationPlanContributor;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;

import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.action.queue.internal.GraphBasedActionQueue;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.audit.ModificationType;
import org.hibernate.collection.spi.PersistentArrayHolder;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.mutation.CollectionAuditSupport;

/// Graph mutation plan contributor for audited basic collection mutations.
///
/// @author Steve Ebersole
public class AuditCollectionMutationPlanContributor implements CollectionMutationPlanContributor {
	private final CollectionPersister persister;
	private final CollectionAuditSupport auditMutationSupport;

	public AuditCollectionMutationPlanContributor(
			CollectionPersister persister,
			SessionFactoryImplementor sessionFactory) {
		this.persister = persister;
		this.auditMutationSupport = new CollectionAuditSupport(
				(CollectionMutationTarget) persister,
				sessionFactory,
				persister.getIndexColumnIsSettable(),
				persister.getElementColumnIsSettable(),
				persister.getIndexIncrementer(),
				persister.getAttributeMapping().getAuditMapping()
		);
	}

	@Override
	public void contributeCollectionChange(
			CollectionChangeContext context,
			Consumer<FlushOperation> operationConsumer) {
		operationConsumer.accept( DecompositionSupport.createNoOpCallbackCarrier(
				context.tableDescriptor(),
				context.ordinalBase() * 1_000 + 900,
				session -> {
					final var collector = resolveCollector( session ).getAuditMutationCollector();
					final var ownerChange = auditMutationSupport.resolveOwnerAuditChange(
							context.key(),
							context.collection(),
							session
					);
					if ( ownerChange != null ) {
						collector.entityChanged(
								ownerChange.entityKey(),
								ownerChange.entity(),
								ownerChange.values(),
								ModificationType.MOD,
								ownerChange.ownerMutationSupport()
						);
					}
					collector.collectionChanged(
							persister,
							context.collection(),
							context.key(),
							resolveSnapshot( context.collection(), context.key(), session ),
							auditMutationSupport
					);
				}
		) );
	}

	private Object resolveSnapshot(
			PersistentCollection<?> collection,
			Object id,
			SessionImplementor session) {
		final var persistenceContext = session.getPersistenceContextInternal();
		final var collectionEntry = persistenceContext.getCollectionEntry( collection );
		if ( collectionEntry != null && collectionEntry.getLoadedPersister() != null ) {
			return collection.getStoredSnapshot();
		}
		else if ( collection instanceof PersistentArrayHolder<?> ) {
			final var oldCollection = persistenceContext.getCollection( new CollectionKey( persister, id ) );
			return oldCollection != null ? oldCollection.getStoredSnapshot() : null;
		}
		return null;
	}

	private GraphBasedActionQueue resolveCollector(SessionImplementor session) {
		final var actionQueue = session.getActionQueue();
		if ( actionQueue instanceof GraphBasedActionQueue graphBasedActionQueue ) {
			return graphBasedActionQueue;
		}
		throw new HibernateException( "Audit graph mutation plan used with non-graph action queue" );
	}
}
