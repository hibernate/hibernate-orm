/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.OptimisticLockHelper;

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
	public void doBeforeTransactionCompletion(SharedSessionContractImplementor session) {
		final var entry = session.getPersistenceContext().getEntry( object );
		// Don't increment the version for an entity that is not in the PersistenceContext
		if ( entry != null ) {
			OptimisticLockHelper.forceVersionIncrement( object, entry, session.asEventSource() );
		}
	}
}
