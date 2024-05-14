/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.lock;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.persister.entity.Lockable;

 /**
 * A locking strategy where an optimistic lock is obtained via a select
  * statement.
 * <p>
 * Differs from {@link PessimisticWriteSelectLockingStrategy} and
 * {@link PessimisticReadSelectLockingStrategy} in throwing
 * {@link OptimisticEntityLockException}.
 *
 * @see org.hibernate.dialect.Dialect#getForUpdateString(LockMode)
 * @see org.hibernate.dialect.Dialect#appendLockHint(LockOptions, String)
 *
 * @author Steve Ebersole
 * @since 3.2
 */
public class SelectLockingStrategy extends AbstractSelectLockingStrategy {
	/**
	 * Construct a locking strategy based on SQL SELECT statements.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indicates the type of lock to be acquired.
	 */
	public SelectLockingStrategy(Lockable lockable, LockMode lockMode) {
		super( lockable, lockMode );
	}

	@Override
	protected HibernateException convertException(Object entity, JDBCException ex) {
		return new OptimisticEntityLockException( entity, "could not obtain optimistic lock", ex );
	}
}
