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
 * Defines the contract for handling of resolve natural id events generated from a session.
 * 
 * @author Eric Dalquist
 * @author Steve Ebersole
 */
public interface ResolveNaturalIdEventListener extends Serializable {

	/**
	 * Handle the given resolve natural id event.
	 * 
	 * @param event The resolve natural id event to be handled.
	 *
	 * @throws HibernateException Indicates a problem resolving natural id to primary key
	 */
	public void onResolveNaturalId(ResolveNaturalIdEvent event) throws HibernateException;

}
