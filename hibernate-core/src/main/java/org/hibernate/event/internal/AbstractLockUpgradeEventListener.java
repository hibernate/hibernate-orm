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
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
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
		final LockMode requestedLockMode = lockOptions.getLockMode();
		if ( requestedLockMode.greaterThan( entry.getLockMode() ) ) {
			// The user requested a "greater" (i.e. more restrictive) form of
			// pessimistic lock

			if ( entry.getStatus() != Status.MANAGED ) {
				throw new ObjectDeletedException(
						"attempted to lock a deleted instance",
						entry.getId(),
						entry.getDescriptor().getEntityName()
				);
			}

			final EntityTypeDescriptor entityDescriptor = entry.getDescriptor();

			if ( log.isTraceEnabled() ) {
				log.tracev(
						"Locking {0} in mode: {1}",
						MessageHelper.infoString( entityDescriptor, entry.getId(), source.getFactory() ),
						requestedLockMode
				);
			}

			final boolean cachingEnabled = entityDescriptor.canWriteToCache();
			SoftLock lock = null;
			Object ck = null;
			try {
				if ( cachingEnabled ) {
					ck = entityDescriptor.getHierarchy().getEntityCacheAccess().generateCacheKey(
							entry.getId(),
							entityDescriptor.getHierarchy(),
							source.getFactory(),
							source.getTenantIdentifier()
					);
					lock = entityDescriptor.getHierarchy().getEntityCacheAccess().lockItem( source, ck, entry.getVersion() );
				}

				if ( entityDescriptor.getHierarchy().getVersionDescriptor() != null
						&& shouldForceVersionIncrement( requestedLockMode ) ) {
					// todo : should we check the current isolation mode explicitly?
					final Object nextVersion = entityDescriptor.forceVersionIncrement(
							entry.getId(),
							entry.getVersion(),
							source
					);
					entry.forceLocked( object, nextVersion );
				}
				else {
					entityDescriptor.lock( entry.getId(), entry.getVersion(), object, lockOptions, source );
				}
				entry.setLockMode( requestedLockMode );
			}
			finally {
				// the database now holds a lock + the object is flushed from the cache,
				// so release the soft lock
				if ( cachingEnabled ) {
					entityDescriptor.getHierarchy().getEntityCacheAccess().unlockItem( source, ck, lock );
				}
			}

		}
	}

	protected boolean shouldForceVersionIncrement(LockMode requestedLockMode) {
		return requestedLockMode == LockMode.FORCE
				|| requestedLockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT
				|| requestedLockMode == LockMode.OPTIMISTIC_FORCE_INCREMENT;
	}
}
