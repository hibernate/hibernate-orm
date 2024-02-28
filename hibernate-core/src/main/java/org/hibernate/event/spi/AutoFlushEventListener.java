/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.HibernateException;
import org.hibernate.SharedSessionContract;

/**
 * Defines the contract for handling of session auto-flush events.
 *
 * @author Steve Ebersole
 */
public interface AutoFlushEventListener {

	/** Handle the given auto-flush event.
	 *
	 * @param event The auto-flush event to be handled.
	 */
	void onAutoFlush(AutoFlushEvent event) throws HibernateException;

	default void onAutoPreFlush(EventSource source) throws HibernateException {
	}
}
