/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of replicate events generated from a session.
 *
 * @author Steve Ebersole
 *
 * @deprecated since {@link org.hibernate.Session#replicate} is deprecated
 */
@Deprecated(since="6")
public interface ReplicateEventListener {

	/** Handle the given replicate event.
	 *
	 * @param event The replicate event to be handled.
	 */
	void onReplicate(ReplicateEvent event) throws HibernateException;

}
