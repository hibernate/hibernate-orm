/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.event.internal;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.TransientObjectException;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.LockEvent;
import org.hibernate.event.spi.LockEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Defines the default lock event listeners used by hibernate to lock entities
 * in response to generated lock events.
 *
 * @author Steve Ebersole
 */
public class DefaultLockEventListener extends AbstractLockUpgradeEventListener implements LockEventListener {

	/** Handle the given lock event.
	 *
	 * @param event The lock event to be handled.
	 * @throws HibernateException
	 */
	public void onLock(LockEvent event) throws HibernateException {

		if ( event.getObject() == null ) {
			throw new NullPointerException( "attempted to lock null" );
		}

		if ( event.getLockMode() == LockMode.WRITE ) {
			throw new HibernateException( "Invalid lock mode for lock()" );
		}

		SessionImplementor source = event.getSession();
		
		Object entity = source.getPersistenceContext().unproxyAndReassociate( event.getObject() );
		//TODO: if object was an uninitialized proxy, this is inefficient,
		//      resulting in two SQL selects
		
		EntityEntry entry = source.getPersistenceContext().getEntry(entity);
		if (entry==null) {
			final EntityPersister persister = source.getEntityPersister( event.getEntityName(), entity );
			final Serializable id = persister.getIdentifier( entity, source );
			if ( !ForeignKeys.isNotTransient( event.getEntityName(), entity, Boolean.FALSE, source ) ) {
				throw new TransientObjectException(
						"cannot lock an unsaved transient instance: " +
						persister.getEntityName()
				);
			}

			entry = reassociate(event, entity, id, persister);
			cascadeOnLock(event, persister, entity);
		}

		upgradeLock( entity, entry, event.getLockOptions(), event.getSession() );
	}
	
	private void cascadeOnLock(LockEvent event, EntityPersister persister, Object entity) {
		EventSource source = event.getSession();
		source.getPersistenceContext().incrementCascadeLevel();
		try {
			new Cascade(CascadingAction.LOCK, Cascade.AFTER_LOCK, source)
					.cascade( persister, entity, event.getLockOptions() );
		}
		finally {
			source.getPersistenceContext().decrementCascadeLevel();
		}
	}

}
