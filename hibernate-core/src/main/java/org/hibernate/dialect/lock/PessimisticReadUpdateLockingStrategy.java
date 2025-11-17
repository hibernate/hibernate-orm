/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.persister.entity.EntityPersister;


/**
 * A pessimistic locking strategy where a lock is obtained via
 * an update statement.
 * <p>
 * This strategy is valid for {@link LockMode#PESSIMISTIC_READ}.
 *
 * @author Steve Ebersole
 * @author Scott Marlow
 * @since 3.5
 */
public class PessimisticReadUpdateLockingStrategy extends AbstractPessimisticUpdateLockingStrategy {

	/**
	 * Construct a locking strategy based on SQL UPDATE statements.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indicates the type of lock to be acquired.  Note that
	 * read-locks are not valid for this strategy.
	 */
	public PessimisticReadUpdateLockingStrategy(EntityPersister lockable, LockMode lockMode) {
		super( lockable, lockMode );
		if ( lockMode.lessThan( LockMode.PESSIMISTIC_READ ) ) {
			throw new HibernateException( "[" + lockMode + "] not valid for update statement" );
		}
	}
}
