/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.dialect.lock;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.event.EventSource;
import org.hibernate.action.EntityIncrementVersionProcess;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.EntityEntry;
import org.hibernate.persister.entity.Lockable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An optimistic locking strategy that forces an increment of the version (after verifying that version hasn't changed).
 * This takes place just prior to transaction commit.
 * <p/>
 * This strategy is valid for LockMode.OPTIMISTIC_FORCE_INCREMENT
 *
 * @since 3.5
 *
 * @author Scott Marlow
 */
public class OptimisticForceIncrementLockingStrategy implements LockingStrategy {
	private static final Logger log = LoggerFactory.getLogger( OptimisticForceIncrementLockingStrategy.class );

	private final Lockable lockable;
	private final LockMode lockMode;
                                             	/**
	 * Construct locking strategy.
	 *
	 * @param lockable The metadata for the entity to be locked.
	 * @param lockMode Indictates the type of lock to be acquired.
	 */
	public OptimisticForceIncrementLockingStrategy(Lockable lockable, LockMode lockMode) {
		this.lockable = lockable;
		this.lockMode = lockMode;
		if ( lockMode.lessThan( LockMode.OPTIMISTIC_FORCE_INCREMENT ) ) {
			throw new HibernateException( "[" + lockMode + "] not valid for [" + lockable.getEntityName() + "]" );
		}
	}

   /**
	 * @see LockingStrategy#lock
	 */
	public void lock(
      Serializable id,
      Object version,
      Object object,
      int timeout, SessionImplementor session) throws StaleObjectStateException, JDBCException {
		if ( !lockable.isVersioned() ) {
			throw new HibernateException( "[" + lockMode + "] not supported for non-versioned entities [" + lockable.getEntityName() + "]" );
		}
      EntityEntry entry = session.getPersistenceContext().getEntry(object);
		EntityIncrementVersionProcess incrementVersion = new EntityIncrementVersionProcess(object, entry);
		EventSource source = (EventSource)session;
		// Register the EntityIncrementVersionProcess action to run just prior to transaction commit. 
		source.getActionQueue().registerProcess(incrementVersion);
	}

	protected LockMode getLockMode() {
		return lockMode;
	}
}