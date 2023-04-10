/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ObjectDeletedException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.loader.LoaderLogging;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public class LoaderHelper {

	public static void upgradeLock(Object object, EntityEntry entry, LockOptions lockOptions, EventSource session) {
		final LockMode requestedLockMode = lockOptions.getLockMode();
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

			if ( LoaderLogging.TRACE_ENABLED ) {
				LoaderLogging.LOADER_LOGGER.tracef(
						"Locking `%s( %s )` in `%s` lock-mode",
						persister.getEntityName(),
						entry.getId(),
						requestedLockMode
				);
			}

			final boolean cachingEnabled = persister.canWriteToCache();
			SoftLock lock = null;
			Object ck = null;
			try {
				if ( cachingEnabled ) {
					EntityDataAccess cache = persister.getCacheAccessStrategy();
					ck = cache.generateCacheKey( entry.getId(), persister, session.getFactory(), session.getTenantIdentifier() );
					lock = cache.lockItem( session, ck, entry.getVersion() );
				}

				if ( persister.isVersioned() && requestedLockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT  ) {
					// todo : should we check the current isolation mode explicitly?
					Object nextVersion = persister.forceVersionIncrement(
							entry.getId(), entry.getVersion(), session
					);
					entry.forceLocked( object, nextVersion );
				}
				else {
					persister.lock( entry.getId(), entry.getVersion(), object, lockOptions, session );
				}
				entry.setLockMode(requestedLockMode);
			}
			finally {
				// the database now holds a lock + the object is flushed from the cache,
				// so release the soft lock
				if ( cachingEnabled ) {
					persister.getCacheAccessStrategy().unlockItem( session, ck, lock );
				}
			}

		}
	}
}
