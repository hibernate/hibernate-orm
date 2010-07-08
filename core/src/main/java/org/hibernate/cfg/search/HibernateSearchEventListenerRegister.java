/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.cfg.search;

import java.util.Properties;

import org.hibernate.AnnotationException;
import org.hibernate.event.EventListeners;
import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PostCollectionRemoveEventListener;
import org.hibernate.event.PostCollectionUpdateEventListener;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.util.ReflectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper methods initializing Hibernate Search event listeners.
 * 
 * @deprecated as of release 3.4.0.CR2, replaced by Hibernate Search's {@link org.hibernate.search.cfg.EventListenerRegister}
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@Deprecated 
public class HibernateSearchEventListenerRegister {

	private static final Logger log = LoggerFactory.getLogger(HibernateSearchEventListenerRegister.class);

	/**
	 * Class name of the class needed to enable Search.
	 */
	private static final String FULL_TEXT_INDEX_EVENT_LISTENER_CLASS = "org.hibernate.search.event.FullTextIndexEventListener";

	/**
	 * @deprecated as of release 3.4.0.CR2, replaced by Hibernate Search's {@link org.hibernate.search.cfg.EventListenerRegister#enableHibernateSearch(EventListeners, Properties)}
	 */
	@SuppressWarnings("unchecked")
	@Deprecated 
	public static void enableHibernateSearch(EventListeners eventListeners, Properties properties) {
		// check whether search is explicitly enabled - if so there is nothing
		// to do
		String enableSearchListeners = properties.getProperty( "hibernate.search.autoregister_listeners" );
		if("false".equalsIgnoreCase(enableSearchListeners )) {
			log.info("Property hibernate.search.autoregister_listeners is set to false." +
					" No attempt will be made to register Hibernate Search event listeners.");
			return;
		}
		
		// add search events if the jar is available and class can be loaded
		Class searchEventListenerClass = attemptToLoadSearchEventListener();
		if ( searchEventListenerClass == null ) {
			log.info("Unable to find {} on the classpath. Hibernate Search is not enabled.", FULL_TEXT_INDEX_EVENT_LISTENER_CLASS);
			return;
		}
		
		Object searchEventListener = instantiateEventListener(searchEventListenerClass);
		
		//TODO Generalize this. Pretty much the same code all the time. Reflecetion? 
		{
			boolean present = false;
			PostInsertEventListener[] listeners = eventListeners
					.getPostInsertEventListeners();
			if (listeners != null) {
				for (Object eventListener : listeners) {
					// not isAssignableFrom since the user could subclass
					present = present
							|| searchEventListenerClass == eventListener
									.getClass();
				}
				if (!present) {
					int length = listeners.length + 1;
					PostInsertEventListener[] newListeners = new PostInsertEventListener[length];
					System.arraycopy(listeners, 0, newListeners, 0, length - 1);
					newListeners[length - 1] = (PostInsertEventListener) searchEventListener;
					eventListeners.setPostInsertEventListeners(newListeners);
				}
			} else {
				eventListeners
						.setPostInsertEventListeners(new PostInsertEventListener[] { (PostInsertEventListener) searchEventListener });
			}
		}
		{
			boolean present = false;
			PostUpdateEventListener[] listeners = eventListeners
					.getPostUpdateEventListeners();
			if (listeners != null) {
				for (Object eventListener : listeners) {
					// not isAssignableFrom since the user could subclass
					present = present
							|| searchEventListenerClass == eventListener
									.getClass();
				}
				if (!present) {
					int length = listeners.length + 1;
					PostUpdateEventListener[] newListeners = new PostUpdateEventListener[length];
					System.arraycopy(listeners, 0, newListeners, 0, length - 1);
					newListeners[length - 1] = (PostUpdateEventListener) searchEventListener;
					eventListeners.setPostUpdateEventListeners(newListeners);
				}
			} else {
				eventListeners
						.setPostUpdateEventListeners(new PostUpdateEventListener[] { (PostUpdateEventListener) searchEventListener });
			}
		}
		{
			boolean present = false;
			PostDeleteEventListener[] listeners = eventListeners
					.getPostDeleteEventListeners();
			if (listeners != null) {
				for (Object eventListener : listeners) {
					// not isAssignableFrom since the user could subclass
					present = present
							|| searchEventListenerClass == eventListener
									.getClass();
				}
				if (!present) {
					int length = listeners.length + 1;
					PostDeleteEventListener[] newListeners = new PostDeleteEventListener[length];
					System.arraycopy(listeners, 0, newListeners, 0, length - 1);
					newListeners[length - 1] = (PostDeleteEventListener) searchEventListener;
					eventListeners.setPostDeleteEventListeners(newListeners);
				}
			} else {
				eventListeners
						.setPostDeleteEventListeners(new PostDeleteEventListener[] { (PostDeleteEventListener) searchEventListener });
			}
		}		
		{
			boolean present = false;
			PostCollectionRecreateEventListener[] listeners = eventListeners.getPostCollectionRecreateEventListeners();
			if ( listeners != null ) {
				for (Object eventListener : listeners) {
					//not isAssignableFrom since the user could subclass
					present = present || searchEventListenerClass == eventListener.getClass();
				}
				if ( !present ) {
					int length = listeners.length + 1;
					PostCollectionRecreateEventListener[] newListeners = new PostCollectionRecreateEventListener[length];
					System.arraycopy( listeners, 0, newListeners, 0, length - 1 );
					newListeners[length - 1] = (PostCollectionRecreateEventListener) searchEventListener;
					eventListeners.setPostCollectionRecreateEventListeners( newListeners );
				}
			}
			else {
				eventListeners.setPostCollectionRecreateEventListeners(
						new PostCollectionRecreateEventListener[] { (PostCollectionRecreateEventListener) searchEventListener }
				);
			}
		}
		{
			boolean present = false;
			PostCollectionRemoveEventListener[] listeners = eventListeners.getPostCollectionRemoveEventListeners();
			if ( listeners != null ) {
				for (Object eventListener : listeners) {
					//not isAssignableFrom since the user could subclass
					present = present || searchEventListenerClass == eventListener.getClass();
				}
				if ( !present ) {
					int length = listeners.length + 1;
					PostCollectionRemoveEventListener[] newListeners = new PostCollectionRemoveEventListener[length];
					System.arraycopy( listeners, 0, newListeners, 0, length - 1 );
					newListeners[length - 1] = (PostCollectionRemoveEventListener) searchEventListener;
					eventListeners.setPostCollectionRemoveEventListeners( newListeners );
				}
			}
			else {
				eventListeners.setPostCollectionRemoveEventListeners(
						new PostCollectionRemoveEventListener[] { (PostCollectionRemoveEventListener) searchEventListener }
				);
			}
		}
		{
			boolean present = false;
			PostCollectionUpdateEventListener[] listeners = eventListeners.getPostCollectionUpdateEventListeners();
			if ( listeners != null ) {
				for (Object eventListener : listeners) {
					//not isAssignableFrom since the user could subclass
					present = present || searchEventListenerClass == eventListener.getClass();
				}
				if ( !present ) {
					int length = listeners.length + 1;
					PostCollectionUpdateEventListener[] newListeners = new PostCollectionUpdateEventListener[length];
					System.arraycopy( listeners, 0, newListeners, 0, length - 1 );
					newListeners[length - 1] = (PostCollectionUpdateEventListener) searchEventListener;
					eventListeners.setPostCollectionUpdateEventListeners( newListeners );
				}
			}
			else {
				eventListeners.setPostCollectionUpdateEventListeners(
						new PostCollectionUpdateEventListener[] { (PostCollectionUpdateEventListener) searchEventListener }
				);
			}
		}		
	}

	/**
	 * Tries to load Hibernate Search event listener.
	 * 
	 * @return An event listener instance in case the jar was available.
	 */
	private static Class<?> attemptToLoadSearchEventListener() {
		Class searchEventListenerClass = null;
		try {
			searchEventListenerClass = ReflectHelper.classForName(
					FULL_TEXT_INDEX_EVENT_LISTENER_CLASS,
					HibernateSearchEventListenerRegister.class);
		} catch (ClassNotFoundException e) {
			log.debug("Search not present in classpath, ignoring event listener registration.");
		}
		return searchEventListenerClass;
	}

	private static Object instantiateEventListener(Class<?> clazz) {
		Object searchEventListener;
		try {
			searchEventListener = clazz.newInstance();
		} catch (Exception e) {
			throw new AnnotationException(
					"Unable to load Search event listener", e);
		}
		return searchEventListener;
	}
}
