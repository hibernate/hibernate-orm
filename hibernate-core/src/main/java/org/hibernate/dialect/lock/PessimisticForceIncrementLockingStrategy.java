/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.dialect.lock;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Lockable;

/**
 * A pessimistic locking strategy that increments the version immediately (obtaining an exclusive write lock).
 * <p/>
 * This strategy is valid for LockMode.PESSIMISTIC_FORCE_INCREMENT
 *
 * @author Scott Marlow
 * @since 3.5
 */
public class PessimisticForceIncrementLockingStrategy implements LockingStrategy {
	private final Lockable lockable;
	private final LockMode lockMode;

	/**
	 * Construct locking strategy.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indicates the type of lock to be acquired.
	 */
	public PessimisticForceIncrementLockingStrategy(Lockable lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		// ForceIncrement can be used for PESSIMISTIC_READ, PESSIMISTIC_WRITE or PESSIMISTIC_FORCE_INCREMENT
		if ( lockMode.lessThan( LockMode.PESSIMISTIC_READ ) ) {
			throw new HibernateException( "[" + lockMode + "] not valid for [" + lockable.getEntityName() + "]" );
		}
	}

	@Override
	public void lock(Serializable id, Object version, Object object, int timeout, SessionImplementor session) {
		if ( !lockable.isVersioned() ) {
			throw new HibernateException( "[" + lockMode + "] not supported for non-versioned entities [" + lockable.getEntityName() + "]" );
		}
		EntityEntry entry = session.getPersistenceContext().getEntry( object );
		final EntityPersister persister = entry.getPersister();
		Object nextVersion = persister.forceVersionIncrement( entry.getId(), entry.getVersion(), session );
		entry.forceLocked( object, nextVersion );
	}

	/**
	 * Retrieve the specific lock mode defined.
	 *
	 * @return The specific lock mode.
	 */
	protected LockMode getLockMode() {
		return lockMode;
	}
}