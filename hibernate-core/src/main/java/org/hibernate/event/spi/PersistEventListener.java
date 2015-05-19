/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of create events generated from a session.
 *
 * @author Gavin King
 */
public interface PersistEventListener extends Serializable {

    /** 
     * Handle the given create event.
     *
     * @param event The create event to be handled.
     * @throws HibernateException
     */
	public void onPersist(PersistEvent event) throws HibernateException;

    /** 
     * Handle the given create event.
     *
     * @param event The create event to be handled.
     * @throws HibernateException
     */
	public void onPersist(PersistEvent event, Map createdAlready) throws HibernateException;

}
