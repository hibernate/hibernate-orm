/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.event.collection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.internal.DefaultInitializeCollectionEventListener;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.InitializeCollectionEvent;
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

/**
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class CollectionListeners {

	public interface Listener extends Serializable {
		void addEvent(AbstractCollectionEvent event, Listener listener);
	}

	public static abstract class AbstractListener implements Listener {

		private final CollectionListeners listeners;

		protected AbstractListener( CollectionListeners listeners ) {
			this.listeners = listeners;
		}

		public void addEvent(AbstractCollectionEvent event, Listener listener) {
			listeners.addEvent( event, listener );
		}
	}

	public static class InitializeCollectionListener
			extends DefaultInitializeCollectionEventListener
			implements Listener {
		private final CollectionListeners listeners;
		private InitializeCollectionListener(CollectionListeners listeners) {
			this.listeners = listeners;
		}
		public void onInitializeCollection(InitializeCollectionEvent event) {
			super.onInitializeCollection( event );
			addEvent( event, this );
		}
		public void addEvent(AbstractCollectionEvent event, Listener listener) {
			listeners.addEvent( event, listener );
		}
	}

	public static class PreCollectionRecreateListener extends AbstractListener
			implements PreCollectionRecreateEventListener {
		private PreCollectionRecreateListener(CollectionListeners listeners) {
			super( listeners );
		}
		public void onPreRecreateCollection(PreCollectionRecreateEvent event) {
			addEvent( event, this );
		}
	}

	public static class PostCollectionRecreateListener extends AbstractListener
			implements PostCollectionRecreateEventListener {
		private PostCollectionRecreateListener(CollectionListeners listeners) {
			super( listeners );
		}
		public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
			addEvent( event, this );
		}
	}

	public static class PreCollectionRemoveListener extends AbstractListener
			implements PreCollectionRemoveEventListener {
		private PreCollectionRemoveListener(CollectionListeners listeners) {
			super( listeners );
		}
		public void onPreRemoveCollection(PreCollectionRemoveEvent event) {
			addEvent( event, this );
		}
	}

	public static class PostCollectionRemoveListener extends AbstractListener
			implements PostCollectionRemoveEventListener {
		private PostCollectionRemoveListener(CollectionListeners listeners) {
			super( listeners );
		}
		public void onPostRemoveCollection(PostCollectionRemoveEvent event) {
			addEvent( event, this );
		}
	}

	public static class PreCollectionUpdateListener extends AbstractListener
			implements PreCollectionUpdateEventListener {
		private PreCollectionUpdateListener(CollectionListeners listeners) {
			super( listeners );
		}
		public void onPreUpdateCollection(PreCollectionUpdateEvent event) {
			addEvent( event, this );
		}
	}

	public static class PostCollectionUpdateListener extends AbstractListener
			implements PostCollectionUpdateEventListener {
		private PostCollectionUpdateListener(CollectionListeners listeners) {
			super( listeners );
		}
		public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
			addEvent( event, this );
		}
	}

	private final PreCollectionRecreateListener preCollectionRecreateListener;
	private final InitializeCollectionListener initializeCollectionListener;
	private final PreCollectionRemoveListener preCollectionRemoveListener;
	private final PreCollectionUpdateListener preCollectionUpdateListener;
	private final PostCollectionRecreateListener postCollectionRecreateListener;
	private final PostCollectionRemoveListener postCollectionRemoveListener;
	private final PostCollectionUpdateListener postCollectionUpdateListener;

	private List listenersCalled = new ArrayList();
	private List events = new ArrayList();

	public CollectionListeners( SessionFactory sf) {
		preCollectionRecreateListener = new PreCollectionRecreateListener( this );
		initializeCollectionListener = new InitializeCollectionListener( this );
		preCollectionRemoveListener = new PreCollectionRemoveListener( this );
		preCollectionUpdateListener = new PreCollectionUpdateListener( this );
		postCollectionRecreateListener = new PostCollectionRecreateListener( this );
		postCollectionRemoveListener = new PostCollectionRemoveListener( this );
		postCollectionUpdateListener = new PostCollectionUpdateListener( this );

		EventListenerRegistry registry = ( (SessionFactoryImplementor) sf ).getServiceRegistry().getService( EventListenerRegistry.class );
		registry.setListeners( EventType.INIT_COLLECTION, initializeCollectionListener );

		registry.setListeners( EventType.PRE_COLLECTION_RECREATE, preCollectionRecreateListener );
		registry.setListeners( EventType.POST_COLLECTION_RECREATE, postCollectionRecreateListener );

		registry.setListeners( EventType.PRE_COLLECTION_REMOVE, preCollectionRemoveListener );
		registry.setListeners( EventType.POST_COLLECTION_REMOVE, postCollectionRemoveListener );

		registry.setListeners( EventType.PRE_COLLECTION_UPDATE, preCollectionUpdateListener );
		registry.setListeners( EventType.POST_COLLECTION_UPDATE, postCollectionUpdateListener );
	}

	public void addEvent(AbstractCollectionEvent event, Listener listener) {
		listenersCalled.add( listener );
		events.add( event );
	}

	public List getListenersCalled() {
		return listenersCalled;
	}

	public List getEvents() {
		return events;
	}

	public void clear() {
		listenersCalled.clear();
		events.clear();
	}

	public PreCollectionRecreateListener getPreCollectionRecreateListener() { return preCollectionRecreateListener; }
	public InitializeCollectionListener getInitializeCollectionListener() { return initializeCollectionListener; }
	public PreCollectionRemoveListener getPreCollectionRemoveListener() { return preCollectionRemoveListener; }
	public PreCollectionUpdateListener getPreCollectionUpdateListener() { return preCollectionUpdateListener; }
	public PostCollectionRecreateListener getPostCollectionRecreateListener() { return postCollectionRecreateListener; }
	public PostCollectionRemoveListener getPostCollectionRemoveListener() { return postCollectionRemoveListener; }
	public PostCollectionUpdateListener getPostCollectionUpdateListener() { return postCollectionUpdateListener; }
}
