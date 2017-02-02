/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.internal.jpa;

import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.PersistenceException;

import org.hibernate.jpa.event.spi.jpa.Listener;
import org.hibernate.jpa.event.spi.jpa.ListenerFactory;

/**
 * Standard implementation of the ListenerFactory contract using simple instantiation.
 *
 * @author Steve Ebersole
 */
public class ListenerFactoryStandardImpl implements ListenerFactory {
	private final ConcurrentHashMap<Class,ListenerImpl> listenerInstances = new ConcurrentHashMap<Class,ListenerImpl>();

	@Override
	@SuppressWarnings("unchecked")
	public <T> Listener<T> buildListener(Class<T> listenerClass) {
		ListenerImpl listenerImpl = listenerInstances.get( listenerClass );
		if ( listenerImpl == null ) {
			try {
				T listenerInstance = listenerClass.newInstance();
				listenerImpl = new ListenerImpl( listenerInstance );
			}
			catch (Exception e) {
				throw new PersistenceException(
						"Unable to create instance of " + listenerClass.getName() + " as a JPA callback listener",
						e
				);
			}
			ListenerImpl existing = listenerInstances.putIfAbsent(
					listenerClass,
					listenerImpl
			);
			if ( existing != null ) {
				listenerImpl = existing;
			}
		}
		return (Listener<T>) listenerImpl;
	}

	@Override
	public void release() {
		listenerInstances.clear();
	}

	private static class ListenerImpl<T> implements Listener<T> {
		private final T listenerInstance;

		public ListenerImpl(T listenerInstance) {
			this.listenerInstance = listenerInstance;
		}

		@Override
		public T getListener() {
			return listenerInstance;
		}
	}
}
