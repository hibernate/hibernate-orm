/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;


/**
 * A locking strategy where a lock is obtained via an update statement.
 * <p>
 * This strategy is not valid for read style locks.
 *
 * @author Steve Ebersole
 * @since 3.2
 *
 * @deprecated No longer used
 */
@Deprecated(since = "7.2", forRemoval = true)
public class UpdateLockingStrategy extends AbstractPessimisticUpdateLockingStrategy {

	/**
	 * Construct a locking strategy based on SQL UPDATE statements.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indicates the type of lock to be acquired.  Note that
	 * read-locks are not valid for this strategy.
	 */
	public UpdateLockingStrategy(EntityPersister lockable, LockMode lockMode) {
		super( lockable, lockMode );
		if ( lockMode.lessThan( LockMode.WRITE ) ) {
			throw new HibernateException( "[" + lockMode + "] not valid for update statement" );
		}
	}

	@Override
	public void lock(Object id, Object version, Object object, int timeout, SharedSessionContractImplementor session)
			throws StaleObjectStateException, JDBCException {
		doLock( id, version, session );
	}
}
