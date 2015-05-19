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
 * Defines the contract for handling of evict events generated from a session.
 *
 * @author Steve Ebersole
 */
public interface EvictEventListener extends Serializable {

    /** 
     * Handle the given evict event.
     *
     * @param event The evict event to be handled.
     * @throws HibernateException
     */
	public void onEvict(EvictEvent event) throws HibernateException;
}
