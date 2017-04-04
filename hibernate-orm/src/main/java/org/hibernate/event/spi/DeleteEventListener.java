/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import java.io.Serializable;
import java.util.Set;

import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of deletion events generated from a session.
 *
 * @author Steve Ebersole
 */
public interface DeleteEventListener extends Serializable {

    /** Handle the given delete event.
     *
     * @param event The delete event to be handled.
     * @throws HibernateException
     */
	public void onDelete(DeleteEvent event) throws HibernateException;

	public void onDelete(DeleteEvent event, Set transientEntities) throws HibernateException;
}
