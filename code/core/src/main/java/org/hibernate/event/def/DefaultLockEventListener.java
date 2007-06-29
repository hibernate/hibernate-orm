//$Id: DefaultLockEventListener.java 7019 2005-06-05 05:09:58Z oneovthafew $
package org.hibernate.event.def;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.TransientObjectException;
import org.hibernate.engine.Cascade;
import org.hibernate.engine.CascadingAction;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.ForeignKeys;
import org.hibernate.event.EventSource;
import org.hibernate.event.LockEvent;
import org.hibernate.event.LockEventListener;
import org.hibernate.engine.SessionImplementor;
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
			final Serializable id = persister.getIdentifier( entity, source.getEntityMode() );
			if ( !ForeignKeys.isNotTransient( event.getEntityName(), entity, Boolean.FALSE, source ) ) {
				throw new TransientObjectException(
						"cannot lock an unsaved transient instance: " +
						persister.getEntityName()
				);
			}

			entry = reassociate(event, entity, id, persister);
			
			cascadeOnLock(event, persister, entity);
		}

		upgradeLock( entity, entry, event.getLockMode(), source );
	}
	
	private void cascadeOnLock(LockEvent event, EntityPersister persister, Object entity) {
		EventSource source = event.getSession();
		source.getPersistenceContext().incrementCascadeLevel();
		try {
			new Cascade(CascadingAction.LOCK, Cascade.AFTER_LOCK, source)
					.cascade( persister, entity, event.getLockMode() );
		}
		finally {
			source.getPersistenceContext().decrementCascadeLevel();
		}
	}

}
