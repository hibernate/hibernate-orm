/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.event.collection.detached;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.InitializeCollectionEvent;
import org.hibernate.event.spi.InitializeCollectionEventListener;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PostCollectionRemoveEvent;
import org.hibernate.event.spi.PostCollectionRemoveEventListener;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PreCollectionRecreateEvent;
import org.hibernate.event.spi.PreCollectionRecreateEventListener;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionRemoveEventListener;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class AggregatedCollectionEventListener
		implements InitializeCollectionEventListener,
				   PreCollectionRecreateEventListener,
				   PostCollectionRecreateEventListener,
				   PreCollectionRemoveEventListener,
				   PostCollectionRemoveEventListener,
				   PreCollectionUpdateEventListener,
				   PostCollectionUpdateEventListener {

	private static final Logger log = Logger.getLogger( AggregatedCollectionEventListener.class );

	private final List<EventEntry> eventEntryList = new ArrayList<EventEntry>();

	public void reset() {
		eventEntryList.clear();
	}

	public List<EventEntry> getEventEntryList() {
		return eventEntryList;
	}

	@Override
	public void onInitializeCollection(InitializeCollectionEvent event) throws HibernateException {
		addEvent( event );
	}

	protected void addEvent(AbstractCollectionEvent event) {
		log.debugf( "Added collection event : %s", event );
		eventEntryList.add( new EventEntry( event ) );
	}


	// recreate ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void onPreRecreateCollection(PreCollectionRecreateEvent event) {
		addEvent( event );
	}

	@Override
	public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
		addEvent( event );
	}


	// remove ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void onPreRemoveCollection(PreCollectionRemoveEvent event) {
		addEvent( event );
	}

	@Override
	public void onPostRemoveCollection(PostCollectionRemoveEvent event) {
		addEvent( event );
	}


	// update ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public void onPreUpdateCollection(PreCollectionUpdateEvent event) {
		addEvent( event );
	}

	@Override
	public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
		addEvent( event );
	}

	public static class EventEntry {
		private final AbstractCollectionEvent event;
		private final Serializable snapshotAtTimeOfEventHandling;

		public EventEntry(AbstractCollectionEvent event) {
			this.event = event;
			// make a copy of the collection?
			this.snapshotAtTimeOfEventHandling = event.getSession()
					.getPersistenceContext()
					.getCollectionEntry( event.getCollection() )
					.getSnapshot();
		}

		public AbstractCollectionEvent getEvent() {
			return event;
		}

		public Serializable getSnapshotAtTimeOfEventHandling() {
			return snapshotAtTimeOfEventHandling;
		}
	}

	public static class IntegratorImpl implements Integrator {
		private AggregatedCollectionEventListener listener;

		public AggregatedCollectionEventListener getListener() {
			if ( listener == null ) {
				throw new HibernateException( "Integrator not yet processed" );
			}
			return listener;
		}

		@Override
		public void integrate(
				Configuration configuration,
				SessionFactoryImplementor sessionFactory,
				SessionFactoryServiceRegistry serviceRegistry) {
			integrate( serviceRegistry );
		}

		protected void integrate(SessionFactoryServiceRegistry serviceRegistry) {
			if ( listener != null ) {
				log.warn( "integrate called second time on testing collection listener Integrator (could be result of rebuilding SF on test failure)" );
			}
			listener = new AggregatedCollectionEventListener();

			final EventListenerRegistry listenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );
			listenerRegistry.appendListeners( EventType.INIT_COLLECTION, listener );
			listenerRegistry.appendListeners( EventType.PRE_COLLECTION_RECREATE, listener );
			listenerRegistry.appendListeners( EventType.POST_COLLECTION_RECREATE, listener );
			listenerRegistry.appendListeners( EventType.PRE_COLLECTION_REMOVE, listener );
			listenerRegistry.appendListeners( EventType.POST_COLLECTION_REMOVE, listener );
			listenerRegistry.appendListeners( EventType.PRE_COLLECTION_UPDATE, listener );
			listenerRegistry.appendListeners( EventType.POST_COLLECTION_UPDATE, listener );
		}


		@Override
		public void integrate(
				MetadataImplementor metadata,
				SessionFactoryImplementor sessionFactory,
				SessionFactoryServiceRegistry serviceRegistry) {
			integrate( serviceRegistry );
		}

		@Override
		public void disintegrate(
				SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
			//To change body of implemented methods use File | Settings | File Templates.
		}
	}
}
