/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.TransientObjectException;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.CascadePoint;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.LockEvent;
import org.hibernate.event.spi.LockEventListener;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * Defines the default lock event listeners used by hibernate to lock entities
 * in response to generated lock events.
 *
 * @author Steve Ebersole
 */
public class DefaultLockEventListener extends AbstractLockUpgradeEventListener implements LockEventListener {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			DefaultLockEventListener.class.getName()
	);

	/**
	 * Handle the given lock event.
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

		if ( event.getLockMode() == LockMode.UPGRADE_SKIPLOCKED ) {
			LOG.explicitSkipLockedLockCombo();
		}

		SessionImplementor source = event.getSession();
		
		Object entity = source.getPersistenceContext().unproxyAndReassociate( event.getObject() );
		//TODO: if object was an uninitialized proxy, this is inefficient,
		//      resulting in two SQL selects
		
		EntityEntry entry = source.getPersistenceContext().getEntry(entity);
		if (entry==null) {
			final EntityTypeDescriptor descriptor = source.getEntityDescriptor( event.getEntityName(), entity );
			final Object id = descriptor.getIdentifier( entity, source );
			if ( !ForeignKeys.isNotTransient( event.getEntityName(), entity, Boolean.FALSE, source ) ) {
				throw new TransientObjectException(
						"cannot lock an unsaved transient instance: " +
						descriptor.getEntityName()
				);
			}

			entry = reassociate(event, entity, id, descriptor);
			cascadeOnLock(event, descriptor, entity);
		}

		upgradeLock( entity, entry, event.getLockOptions(), event.getSession() );
	}
	
	private void cascadeOnLock(LockEvent event, EntityTypeDescriptor descriptor, Object entity) {
		EventSource source = event.getSession();
		source.getPersistenceContext().incrementCascadeLevel();
		try {
			Cascade.cascade(
					CascadingActions.LOCK,
					CascadePoint.AFTER_LOCK,
					source,
					descriptor,
					entity,
					event.getLockOptions()
			);
		}
		finally {
			source.getPersistenceContext().decrementCascadeLevel();
		}
	}

}
