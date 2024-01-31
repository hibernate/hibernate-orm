/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of update events generated from a session.
 *
 * @author Steve Ebersole
 *
 * @deprecated since {@link org.hibernate.Session#saveOrUpdate} and friends are deprecated
 */
@Deprecated(since="6")
public interface SaveOrUpdateEventListener {

	/** 
	 * Handle the given update event.
	 *
	 * @param event The update event to be handled.
	 */
	void onSaveOrUpdate(SaveOrUpdateEvent event) throws HibernateException;

}
