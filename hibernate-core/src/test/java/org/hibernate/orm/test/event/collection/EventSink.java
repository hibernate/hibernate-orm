/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection;

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
public class EventSink {
	private final List<Listener> listenersCalled = new ArrayList<>();
	private final List<AbstractCollectionEvent> events = new ArrayList<>();

	private final InitializationListener initializationListener;
	private final PreRecreateListener preRecreateListener;
	private final PostRecreateListener postRecreateListener;
	private final PreRemoveListener preRemoveListener;
	private final PostRemoveListener postRemoveListener;
	private final PreUpdateListener preUpdateListener;
	private final PostUpdateListener postUpdateListener;

	public EventSink(SessionFactory sf) {
		preRecreateListener = new PreRecreateListener( this );
		initializationListener = new InitializationListener( this );
		preRemoveListener = new PreRemoveListener( this );
		preUpdateListener = new PreUpdateListener( this );
		postRecreateListener = new PostRecreateListener( this );
		postRemoveListener = new PostRemoveListener( this );
		postUpdateListener = new PostUpdateListener( this );

		EventListenerRegistry registry = ( (SessionFactoryImplementor) sf ).getEventListenerRegistry();
		registry.setListeners( EventType.INIT_COLLECTION, initializationListener );

		registry.setListeners( EventType.PRE_COLLECTION_RECREATE, preRecreateListener );
		registry.setListeners( EventType.POST_COLLECTION_RECREATE, postRecreateListener );

		registry.setListeners( EventType.PRE_COLLECTION_REMOVE, preRemoveListener );
		registry.setListeners( EventType.POST_COLLECTION_REMOVE, postRemoveListener );

		registry.setListeners( EventType.PRE_COLLECTION_UPDATE, preUpdateListener );
		registry.setListeners( EventType.POST_COLLECTION_UPDATE, postUpdateListener );
	}

	public List<Listener> getListenersCalled() {
		return listenersCalled;
	}

	public List<AbstractCollectionEvent> getEvents() {
		return events;
	}

	private void addEvent(AbstractCollectionEvent event, Listener listener) {
		listenersCalled.add( listener );
		events.add( event );
	}

	public void clear() {
		listenersCalled.clear();
		events.clear();
	}

	public PreRecreateListener getPreCollectionRecreateListener() { return preRecreateListener; }
	public InitializationListener getInitializeCollectionListener() { return initializationListener; }
	public PreRemoveListener getPreCollectionRemoveListener() { return preRemoveListener; }
	public PreUpdateListener getPreCollectionUpdateListener() { return preUpdateListener; }
	public PostRecreateListener getPostCollectionRecreateListener() { return postRecreateListener; }
	public PostRemoveListener getPostCollectionRemoveListener() { return postRemoveListener; }
	public PostUpdateListener getPostCollectionUpdateListener() { return postUpdateListener; }



	public interface Listener extends Serializable {
	}

	public static abstract class AbstractListener implements Listener {
		private final EventSink listeners;

		protected AbstractListener(EventSink listeners) {
			this.listeners = listeners;
		}

		public void addEvent(AbstractCollectionEvent event, Listener listener) {
			listeners.addEvent( event, listener );
		}
	}

	public static class InitializationListener
			extends DefaultInitializeCollectionEventListener
			implements Listener {
		private final EventSink listeners;
		private InitializationListener(EventSink listeners) {
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

	public static class PreRecreateListener extends AbstractListener
			implements PreCollectionRecreateEventListener {
		private PreRecreateListener(EventSink listeners) {
			super( listeners );
		}
		public void onPreRecreateCollection(PreCollectionRecreateEvent event) {
			addEvent( event, this );
		}
	}

	public static class PostRecreateListener extends AbstractListener
			implements PostCollectionRecreateEventListener {
		private PostRecreateListener(EventSink listeners) {
			super( listeners );
		}
		public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
			addEvent( event, this );
		}
	}

	public static class PreRemoveListener extends AbstractListener
			implements PreCollectionRemoveEventListener {
		private PreRemoveListener(EventSink listeners) {
			super( listeners );
		}
		public void onPreRemoveCollection(PreCollectionRemoveEvent event) {
			addEvent( event, this );
		}
	}

	public static class PostRemoveListener extends AbstractListener
			implements PostCollectionRemoveEventListener {
		private PostRemoveListener(EventSink listeners) {
			super( listeners );
		}
		public void onPostRemoveCollection(PostCollectionRemoveEvent event) {
			addEvent( event, this );
		}
	}

	public static class PreUpdateListener extends AbstractListener
			implements PreCollectionUpdateEventListener {
		private PreUpdateListener(EventSink listeners) {
			super( listeners );
		}
		public void onPreUpdateCollection(PreCollectionUpdateEvent event) {
			addEvent( event, this );
		}
	}

	public static class PostUpdateListener extends AbstractListener
			implements PostCollectionUpdateEventListener {
		private PostUpdateListener(EventSink listeners) {
			super( listeners );
		}
		public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
			addEvent( event, this );
		}
	}
}
