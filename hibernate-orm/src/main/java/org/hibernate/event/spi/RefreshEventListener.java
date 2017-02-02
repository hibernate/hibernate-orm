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
 * Defines the contract for handling of refresh events generated from a session.
 *
 * @author Steve Ebersole
 */
public interface RefreshEventListener extends Serializable {

    /** 
     * Handle the given refresh event.
     *
     * @param event The refresh event to be handled.
     * @throws HibernateException
     */
	public void onRefresh(RefreshEvent event) throws HibernateException;
	
	public void onRefresh(RefreshEvent event, Map refreshedAlready) throws HibernateException;

}
