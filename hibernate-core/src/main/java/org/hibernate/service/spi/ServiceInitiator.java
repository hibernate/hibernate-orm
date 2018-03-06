/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service.spi;

import org.hibernate.service.Service;

/**
 * Base contract for an initiator of a service.
 *
 * @author Steve Ebersole
 */
public interface ServiceInitiator<R extends Service> {
	/**
	 * Obtains the service role initiated by this initiator.  Should be unique within a registry
	 *
	 * @return The service role.
	 */
	Class<R> getServiceInitiated();
}
