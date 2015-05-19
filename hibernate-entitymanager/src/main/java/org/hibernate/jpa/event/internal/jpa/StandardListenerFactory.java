/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.internal.jpa;

import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.PersistenceException;

import org.hibernate.jpa.event.spi.jpa.ListenerFactory;

/**
 * Standard implementation of the ListenerFactory contract using simple instantiation.  Listener instances
 * are kept in a map keyed by Class to achieve singleton-ness.
 *
 * @author Steve Ebersole
 */
public class StandardListenerFactory implements ListenerFactory {

	private final ConcurrentHashMap listenerInstances = new ConcurrentHashMap();

	@Override
	@SuppressWarnings("unchecked")
	public <T> T buildListener(Class<T> listenerClass) {
		Object listenerInstance = listenerInstances.get( listenerClass );
		if ( listenerInstance == null ) {
			try {
				listenerInstance = listenerClass.newInstance();
			}
			catch (Exception e) {
				throw new PersistenceException(
						"Unable to create instance of " + listenerClass.getName() + " as a JPA callback listener",
						e
				);
			}
			Object existing = listenerInstances.putIfAbsent( listenerClass, listenerInstance );
			if ( existing != null ) {
				listenerInstance = existing;
			}
		}
		return (T) listenerInstance;
	}

	@Override
	public void release() {
		listenerInstances.clear();
	}
}
