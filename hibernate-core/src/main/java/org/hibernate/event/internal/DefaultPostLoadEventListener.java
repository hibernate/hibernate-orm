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
package org.hibernate.event.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.LockMode;
import org.hibernate.action.internal.EntityIncrementVersionProcess;
import org.hibernate.action.internal.EntityVerifyVersionProcess;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * We do 2 things here:<ul>
 * <li>Call {@link Lifecycle} interface if necessary</li>
 * <li>Perform needed {@link EntityEntry#getLockMode()} related processing</li>
 * </ul>
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DefaultPostLoadEventListener implements PostLoadEventListener {
	
	public void onPostLoad(PostLoadEvent event) {
		final Object entity = event.getEntity();
		final EntityEntry entry = event.getSession().getPersistenceContext().getEntry( entity );
		if ( entry == null ) {
			throw new AssertionFailure( "possible non-threadsafe access to the session" );
		}

		final LockMode lockMode = entry.getLockMode();
		if ( LockMode.PESSIMISTIC_FORCE_INCREMENT.equals( lockMode ) ) {
			final EntityPersister persister = entry.getPersister();
			Object nextVersion = persister.forceVersionIncrement(
					entry.getId(), entry.getVersion(), event.getSession()
			);
			entry.forceLocked( entity, nextVersion );
		}
		else if ( LockMode.OPTIMISTIC_FORCE_INCREMENT.equals( lockMode ) ) {
			EntityIncrementVersionProcess incrementVersion = new EntityIncrementVersionProcess( entity, entry );
			event.getSession().getActionQueue().registerProcess( incrementVersion );
		}
		else if ( LockMode.OPTIMISTIC.equals( lockMode ) ) {
			EntityVerifyVersionProcess verifyVersion = new EntityVerifyVersionProcess( entity, entry );
			event.getSession().getActionQueue().registerProcess( verifyVersion );
		}

		if ( event.getPersister().implementsLifecycle() ) {
			//log.debug( "calling onLoad()" );
			( ( Lifecycle ) event.getEntity() ).onLoad( event.getSession(), event.getId() );
		}

	}
}
