/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service.spi;

import org.hibernate.service.Service;

/**
 * Contract for an initiator of services that target the specialized service registry
 * {@link SessionFactoryServiceRegistry}
 *
 * @author Steve Ebersole
 */
public interface SessionFactoryServiceInitiator<R extends Service> extends ServiceInitiator<R>{
	/**
	 * Initiates the managed service.
	 *
	 * @param context Access to initialization contextual info
	 *
	 * @return The initiated service.
	 */
	R initiateService(SessionFactoryServiceInitiatorContext context);

}
