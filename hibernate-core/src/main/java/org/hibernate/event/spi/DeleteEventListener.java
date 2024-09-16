/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;


import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of deletion events generated from a session.
 *
 * @author Steve Ebersole
 */
public interface DeleteEventListener {

	/**
	 * Handle the given delete event.
	 *
	 * @param event The delete event to be handled.
	 */
	void onDelete(DeleteEvent event) throws HibernateException;

	void onDelete(DeleteEvent event, DeleteContext transientEntities) throws HibernateException;
}
