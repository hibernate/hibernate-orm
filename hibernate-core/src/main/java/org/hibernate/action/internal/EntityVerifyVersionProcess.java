/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.dialect.lock.internal.OptimisticEntityLockException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import static org.hibernate.pretty.MessageHelper.infoString;

/**
 * A {@link BeforeTransactionCompletionProcess} impl to verify an entity
 * version as part of before-transaction-completion processing.
 * <p>
 * The current version is read from the database using the persister's
 * version select, which is rendered as a "current read": on databases
 * where a plain read would not wait for a concurrent uncommitted write
 * to the row, the dialect renders whatever makes the read wait and see
 * the current version rather than a snapshot.
 *
 * @see org.hibernate.dialect.lock.spi.LockingSupport#renderCurrentReadClause()
 * @see org.hibernate.dialect.lock.spi.LockingSupport.Metadata#readsWaitForUncommittedWrites()
 *
 * @author Scott Marlow
 */
public class EntityVerifyVersionProcess implements BeforeTransactionCompletionProcess {

	private final Object object;

	/**
	 * Constructs an EntityVerifyVersionProcess
	 *
	 * @param object The entity instance
	 */
	public EntityVerifyVersionProcess(Object object) {
		this.object = object;
	}

	@Override
	public void doBeforeTransactionCompletion(SharedSessionContractImplementor session) {
		final var entry = session.getPersistenceContext().getEntry( object );
		// Don't check the version for an entity that is not in the PersistenceContext
		if ( entry != null ) {
			final Object latestVersion = entry.getPersister().getCurrentVersion( entry.getId(), session );
			if ( !entry.getVersion().equals( latestVersion ) ) {
				throw new OptimisticEntityLockException(
						object,
						"Newer version ["
								+ latestVersion
								+ "] of entity ["
								+ infoString( entry.getEntityName(), entry.getId() )
								+ "] found in database"
				);
			}
		}
	}
}
