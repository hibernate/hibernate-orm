/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.OptimisticLockHelper;
import org.hibernate.persister.entity.EntityPersister;

/**
 * A {@link BeforeTransactionCompletionProcess} implementation to verify and
 * increment an entity version as party of before-transaction-completion
 * processing.
 *
 * @author Scott Marlow
 */
public class EntityIncrementVersionProcess implements BeforeTransactionCompletionProcess {
	private final Object object;
	private final Object lockVersion;

	/**
	 * Constructs an EntityIncrementVersionProcess for the given entity.
	 *
	 * @param object The entity instance
	 * @param lockVersion The entity version at lock time
	 */
	public EntityIncrementVersionProcess(Object object, Object lockVersion) {
		this.object = object;
		this.lockVersion = lockVersion;
	}

	/**
	 * Perform whatever processing is encapsulated here before completion of the transaction.
	 *
	 * @param session The session on which the transaction is preparing to complete.
	 */
	@Override
	public void doBeforeTransactionCompletion(SharedSessionContractImplementor session) {
		final var entry = session.getPersistenceContext().getEntry( object );
		// Don't increment the version for an entity that is not in the PersistenceContext
		if ( entry != null ) {
			if ( lockVersion != null ) {
				final EntityPersister persister = entry.getPersister();
				if ( persister.isVersioned() ) {
					// if the entity version is different than the version we locked, the version
					// was already incremented (e.g. via flushing dirty properties)
					if ( !persister.getVersionType().isEqual( entry.getVersion(), lockVersion ) ) {
						return;
					}
				}
			}
			OptimisticLockHelper.forceVersionIncrement( object, entry, session.asEventSource() );
		}
	}
}
