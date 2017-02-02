/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.service.spi;

import org.hibernate.HibernateException;

/**
 * Indicates a problem registering an event listener.
 * 
 * @author Steve Ebersole
 */
public class EventListenerRegistrationException extends HibernateException {
	public EventListenerRegistrationException(String s) {
		super( s );
	}

	public EventListenerRegistrationException(String string, Throwable root) {
		super( string, root );
	}
}
