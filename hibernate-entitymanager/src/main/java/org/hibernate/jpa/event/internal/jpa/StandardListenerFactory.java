/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
