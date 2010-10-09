//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution statements
 * applied by the authors.
 *
 * All third-party contributions are distributed under license by Red Hat
 * Middleware LLC.  This copyrighted material is made available to anyone
 * wishing to use, modify, copy, or redistribute it subject to the terms
 * and conditions of the GNU Lesser General Public License, as published by
 * the Free Software Foundation.  This program is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the GNU Lesser General Public License for more details.  You should
 * have received a copy of the GNU Lesser General Public License along with
 * this distribution; if not, write to: Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor Boston, MA  02110-1301  USA
 */
package org.hibernate.test.event.collection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.event.AbstractCollectionEvent;
import org.hibernate.event.InitializeCollectionEvent;
import org.hibernate.event.InitializeCollectionEventListener;
import org.hibernate.event.PostCollectionRecreateEvent;
import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PostCollectionRemoveEvent;
import org.hibernate.event.PostCollectionRemoveEventListener;
import org.hibernate.event.PostCollectionUpdateEvent;
import org.hibernate.event.PostCollectionUpdateEventListener;
import org.hibernate.event.PreCollectionRecreateEvent;
import org.hibernate.event.PreCollectionRecreateEventListener;
import org.hibernate.event.PreCollectionRemoveEvent;
import org.hibernate.event.PreCollectionRemoveEventListener;
import org.hibernate.event.PreCollectionUpdateEvent;
import org.hibernate.event.PreCollectionUpdateEventListener;
import org.hibernate.event.def.DefaultInitializeCollectionEventListener;
import org.hibernate.impl.SessionFactoryImpl;

/**
 * Author: Gail Badner
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
		SessionFactoryImpl impl = ( SessionFactoryImpl ) sf;
		impl.getEventListeners().setInitializeCollectionEventListeners(
				new InitializeCollectionEventListener[] { initializeCollectionListener }
		);
		impl.getEventListeners().setPreCollectionRecreateEventListeners(
				new PreCollectionRecreateEventListener[] { preCollectionRecreateListener }
		);
		impl.getEventListeners().setPostCollectionRecreateEventListeners(
				new PostCollectionRecreateEventListener[] { postCollectionRecreateListener }
		);
		impl.getEventListeners().setPreCollectionRemoveEventListeners(
				new PreCollectionRemoveEventListener[] { preCollectionRemoveListener }
		);
		impl.getEventListeners().setPostCollectionRemoveEventListeners(
				new PostCollectionRemoveEventListener[] { postCollectionRemoveListener }
		);
		impl.getEventListeners().setPreCollectionUpdateEventListeners(
				new PreCollectionUpdateEventListener[] { preCollectionUpdateListener }
		);
		impl.getEventListeners().setPostCollectionUpdateEventListeners(
				new PostCollectionUpdateEventListener[] { postCollectionUpdateListener }
		);
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
