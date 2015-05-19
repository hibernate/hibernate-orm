/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;

import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * A BeforeTransactionCompletionProcess impl to verify and increment an entity version as party
 * of before-transaction-completion processing
 *
 * @author Scott Marlow
 */
public class EntityIncrementVersionProcess implements BeforeTransactionCompletionProcess {
	private final Object object;
	private final EntityEntry entry;

	/**
	 * Constructs an EntityIncrementVersionProcess for the given entity.
	 *
	 * @param object The entity instance
	 * @param entry The entity's EntityEntry reference
	 */
	public EntityIncrementVersionProcess(Object object, EntityEntry entry) {
		this.object = object;
		this.entry = entry;
	}

	/**
	 * Perform whatever processing is encapsulated here before completion of the transaction.
	 *
	 * @param session The session on which the transaction is preparing to complete.
	 */
	@Override
	public void doBeforeTransactionCompletion(SessionImplementor session) {
		final EntityPersister persister = entry.getPersister();
		final Object nextVersion = persister.forceVersionIncrement( entry.getId(), entry.getVersion(), session );
		entry.forceLocked( object, nextVersion );
	}
}
