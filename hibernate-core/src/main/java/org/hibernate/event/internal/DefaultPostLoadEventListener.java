/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;


import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.action.internal.EntityIncrementVersionProcess;
import org.hibernate.action.internal.EntityVerifyVersionProcess;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.jpa.event.spi.CallbackRegistry;
import org.hibernate.jpa.event.spi.CallbackRegistryConsumer;
import org.hibernate.persister.entity.EntityPersister;

/**
 * We do two things here:
 * <ul>
 * <li>Call {@link Lifecycle} interface if necessary</li>
 * <li>Perform needed {@link EntityEntry#getLockMode()} related processing</li>
 * </ul>
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DefaultPostLoadEventListener implements PostLoadEventListener, CallbackRegistryConsumer {
	private CallbackRegistry callbackRegistry;

	@Override
	public void injectCallbackRegistry(CallbackRegistry callbackRegistry) {
		this.callbackRegistry = callbackRegistry;
	}

	@Override
	public void onPostLoad(PostLoadEvent event) {
		final Object entity = event.getEntity();

		callbackRegistry.postLoad( entity );

		final EventSource session = event.getSession();
		final EntityEntry entry = session.getPersistenceContextInternal().getEntry( entity );
		if ( entry == null ) {
			throw new AssertionFailure( "possible non-threadsafe access to the session" );
		}

		final LockMode lockMode = entry.getLockMode();
		if ( lockMode.requiresVersion() ) {
			final EntityPersister persister = entry.getPersister();
			if ( persister.isVersioned() ) {
				switch ( lockMode ) {
					case PESSIMISTIC_FORCE_INCREMENT:
						final Object nextVersion =
								persister.forceVersionIncrement( entry.getId(), entry.getVersion(), false, session );
						entry.forceLocked( entity, nextVersion );
						break;
					case OPTIMISTIC_FORCE_INCREMENT:
						session.getActionQueue().registerProcess( new EntityIncrementVersionProcess( entity ) );
						break;
					case OPTIMISTIC:
						session.getActionQueue().registerProcess( new EntityVerifyVersionProcess( entity ) );
						break;
				}
			}
			else {
				throw new HibernateException("[" + lockMode
						+ "] not supported for non-versioned entities [" + persister.getEntityName() + "]");
			}
		}

		invokeLoadLifecycle( event, session );
	}

	protected void invokeLoadLifecycle(PostLoadEvent event, EventSource session) {
		if ( event.getPersister().implementsLifecycle() ) {
			( (Lifecycle) event.getEntity() ).onLoad( session, event.getId() );
		}
	}
}
