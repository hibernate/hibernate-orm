/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import java.io.Serializable;

import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of lock events generated from a session.
 *
 * @author Steve Ebersole
 */
public interface LockEventListener extends Serializable {

    /** Handle the given lock event.
     *
     * @param event The lock event to be handled.
     * @throws HibernateException
     */
	public void onLock(LockEvent event) throws HibernateException;
}
