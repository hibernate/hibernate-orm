/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.detached;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.CollectionEntry;
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

import org.jboss.logging.Logger;

/**
 * Support listeners for Test HHH-6361: Collection events may contain wrong
 * stored snapshot after merging a detached entity into the persistencecontext.
 *
 * @author Erik-Berndt Scheper
 */
public class MultipleCollectionListeners {

	private final Logger log = Logger.getLogger(MultipleCollectionListeners.class);

	public interface Listener extends Serializable {
		void addEvent(AbstractCollectionEvent event, Listener listener);
	}

	public static abstract class AbstractListener implements Listener {

		private final MultipleCollectionListeners listeners;

		protected AbstractListener(MultipleCollectionListeners listeners) {
			this.listeners = listeners;
		}

		public void addEvent(AbstractCollectionEvent event, Listener listener) {
			listeners.addEvent(event, listener);
		}
	}

	public static class InitializeCollectionListener extends
			DefaultInitializeCollectionEventListener implements Listener {
		private final MultipleCollectionListeners listeners;

		private InitializeCollectionListener(
				MultipleCollectionListeners listeners) {
			this.listeners = listeners;
		}

		public void onInitializeCollection(InitializeCollectionEvent event) {
			super.onInitializeCollection(event);
			addEvent(event, this);
		}

		public void addEvent(AbstractCollectionEvent event, Listener listener) {
			listeners.addEvent(event, listener);
		}
	}

	public static class PreCollectionRecreateListener extends AbstractListener
			implements PreCollectionRecreateEventListener {
		private PreCollectionRecreateListener(
				MultipleCollectionListeners listeners) {
			super(listeners);
		}

		public void onPreRecreateCollection(PreCollectionRecreateEvent event) {
			addEvent(event, this);
		}
	}

	public static class PostCollectionRecreateListener extends AbstractListener
			implements PostCollectionRecreateEventListener {
		private PostCollectionRecreateListener(
				MultipleCollectionListeners listeners) {
			super(listeners);
		}

		public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
			addEvent(event, this);
		}
	}

	public static class PreCollectionRemoveListener extends AbstractListener
			implements PreCollectionRemoveEventListener {
		private PreCollectionRemoveListener(
				MultipleCollectionListeners listeners) {
			super(listeners);
		}

		public void onPreRemoveCollection(PreCollectionRemoveEvent event) {
			addEvent(event, this);
		}
	}

	public static class PostCollectionRemoveListener extends AbstractListener
			implements PostCollectionRemoveEventListener {
		private PostCollectionRemoveListener(
				MultipleCollectionListeners listeners) {
			super(listeners);
		}

		public void onPostRemoveCollection(PostCollectionRemoveEvent event) {
			addEvent(event, this);
		}
	}

	public static class PreCollectionUpdateListener extends AbstractListener
			implements PreCollectionUpdateEventListener {
		private PreCollectionUpdateListener(
				MultipleCollectionListeners listeners) {
			super(listeners);
		}

		public void onPreUpdateCollection(PreCollectionUpdateEvent event) {
			addEvent(event, this);
		}
	}

	public static class PostCollectionUpdateListener extends AbstractListener
			implements PostCollectionUpdateEventListener {
		private PostCollectionUpdateListener(
				MultipleCollectionListeners listeners) {
			super(listeners);
		}

		public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
			addEvent(event, this);
		}
	}

	private final PreCollectionRecreateListener preCollectionRecreateListener;
	private final InitializeCollectionListener initializeCollectionListener;
	private final PreCollectionRemoveListener preCollectionRemoveListener;
	private final PreCollectionUpdateListener preCollectionUpdateListener;
	private final PostCollectionRecreateListener postCollectionRecreateListener;
	private final PostCollectionRemoveListener postCollectionRemoveListener;
	private final PostCollectionUpdateListener postCollectionUpdateListener;

	private List<Listener> listenersCalled = new ArrayList<Listener>();
	private List<AbstractCollectionEvent> events = new ArrayList<AbstractCollectionEvent>();
	private List<Serializable> snapshots = new ArrayList<Serializable>();

	public MultipleCollectionListeners(SessionFactory sf) {
		preCollectionRecreateListener = new PreCollectionRecreateListener(this);
		initializeCollectionListener = new InitializeCollectionListener(this);
		preCollectionRemoveListener = new PreCollectionRemoveListener(this);
		preCollectionUpdateListener = new PreCollectionUpdateListener(this);
		postCollectionRecreateListener = new PostCollectionRecreateListener(
				this);
		postCollectionRemoveListener = new PostCollectionRemoveListener(this);
		postCollectionUpdateListener = new PostCollectionUpdateListener(this);
		EventListenerRegistry registry = ( (SessionFactoryImplementor) sf ).getEventListenerRegistry();
		registry.setListeners( EventType.INIT_COLLECTION, initializeCollectionListener );

		registry.setListeners( EventType.PRE_COLLECTION_RECREATE, preCollectionRecreateListener );
		registry.setListeners( EventType.POST_COLLECTION_RECREATE, postCollectionRecreateListener );

		registry.setListeners( EventType.PRE_COLLECTION_REMOVE, preCollectionRemoveListener );
		registry.setListeners( EventType.POST_COLLECTION_REMOVE, postCollectionRemoveListener );

		registry.setListeners( EventType.PRE_COLLECTION_UPDATE, preCollectionUpdateListener );
		registry.setListeners( EventType.POST_COLLECTION_UPDATE, postCollectionUpdateListener );
	}

	public void addEvent(AbstractCollectionEvent event, Listener listener) {


		CollectionEntry collectionEntry = event.getSession()
				.getPersistenceContext()
				.getCollectionEntry(event.getCollection());
		Serializable snapshot = collectionEntry.getSnapshot();

		log.debug("add Event: " + event.getClass() + "; listener = "
				+ listener.getClass() + "; snapshot = " + snapshot);

		listenersCalled.add(listener);
		events.add(event);
		snapshots.add(snapshot);
	}

	public List<Listener> getListenersCalled() {
		return listenersCalled;
	}

	public List<AbstractCollectionEvent> getEvents() {
		return events;
	}

	public List<Serializable> getSnapshots() {
		return snapshots;
	}

	public void clear() {
		listenersCalled.clear();
		events.clear();
		snapshots.clear();
	}

	public PreCollectionRecreateListener getPreCollectionRecreateListener() {
		return preCollectionRecreateListener;
	}

	public InitializeCollectionListener getInitializeCollectionListener() {
		return initializeCollectionListener;
	}

	public PreCollectionRemoveListener getPreCollectionRemoveListener() {
		return preCollectionRemoveListener;
	}

	public PreCollectionUpdateListener getPreCollectionUpdateListener() {
		return preCollectionUpdateListener;
	}

	public PostCollectionRecreateListener getPostCollectionRecreateListener() {
		return postCollectionRecreateListener;
	}

	public PostCollectionRemoveListener getPostCollectionRemoveListener() {
		return postCollectionRemoveListener;
	}

	public PostCollectionUpdateListener getPostCollectionUpdateListener() {
		return postCollectionUpdateListener;
	}
}
