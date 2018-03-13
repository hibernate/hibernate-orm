/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ObjectDeletedException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.jboss.logging.Logger;

/**
 * A convenience base class for listeners that respond to requests to perform a
 * pessimistic lock upgrade on an entity.
 *
 * @author Gavin King
 */
public abstract class AbstractLockUpgradeEventListener extends AbstractReassociateEventListener {
	private static final Logger log = CoreLogging.logger( AbstractLockUpgradeEventListener.class );

	/**
	 * Performs a pessimistic lock upgrade on a given entity, if needed.
	 *
	 * @param object The entity for which to upgrade the lock.
	 * @param entry The entity's EntityEntry instance.
	 * @param lockOptions contains the requested lock mode.
	 * @param source The session which is the source of the event being processed.
	 */
	protected void upgradeLock(Object object, EntityEntry entry, LockOptions lockOptions, EventSource source) {

		LockMode requestedLockMode = lockOptions.getLockMode();
		if ( requestedLockMode.greaterThan( entry.getLockMode() ) ) {
			// The user requested a "greater" (i.e. more restrictive) form of
			// pessimistic lock

			if ( entry.getStatus() != Status.MANAGED ) {
				throw new ObjectDeletedException(
						"attempted to lock a deleted instance",
						entry.getId(),
						entry.getPersister().getEntityName()
				);
			}

			final EntityPersister persister = entry.getPersister();

			if ( log.isTraceEnabled() ) {
				log.tracev(
						"Locking {0} in mode: {1}",
						MessageHelper.infoString( persister, entry.getId(), source.getFactory() ),
						requestedLockMode
				);
			}

			final boolean cachingEnabled = persister.canWriteToCache();
			SoftLock lock = null;
			Object ck = null;
			try {
				if ( cachingEnabled ) {
					EntityDataAccess cache = persister.getCacheAccessStrategy();
					ck = cache.generateCacheKey( entry.getId(), persister, source.getFactory(), source.getTenantIdentifier() );
					lock = cache.lockItem( source, ck, entry.getVersion() );
				}

				if ( persister.isVersioned() && requestedLockMode == LockMode.FORCE  ) {
					// todo : should we check the current isolation mode explicitly?
					Object nextVersion = persister.forceVersionIncrement(
							entry.getId(), entry.getVersion(), source
					);
					entry.forceLocked( object, nextVersion );
				}
				else {
					persister.lock( entry.getId(), entry.getVersion(), object, lockOptions, source );
				}
				entry.setLockMode(requestedLockMode);
			}
			finally {
				// the database now holds a lock + the object is flushed from the cache,
				// so release the soft lock
				if ( cachingEnabled ) {
					persister.getCacheAccessStrategy().unlockItem( source, ck, lock );
				}
			}

		}
	}
}
