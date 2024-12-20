/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
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

	/**
	 * Constructs an EntityIncrementVersionProcess for the given entity.
	 *
	 * @param object The entity instance
	 */
	public EntityIncrementVersionProcess(Object object) {
		this.object = object;
	}

	/**
	 * Perform whatever processing is encapsulated here before completion of the transaction.
	 *
	 * @param session The session on which the transaction is preparing to complete.
	 */
	@Override
	public void doBeforeTransactionCompletion(SessionImplementor session) {
		final EntityEntry entry = session.getPersistenceContext().getEntry( object );
		// Don't increment version for an entity that is not in the PersistenceContext;
		if ( entry == null ) {
			return;
		}

		final EntityPersister persister = entry.getPersister();
		final Object nextVersion = persister.forceVersionIncrement( entry.getId(), entry.getVersion(), session );
		entry.forceLocked( object, nextVersion );
	}
}
