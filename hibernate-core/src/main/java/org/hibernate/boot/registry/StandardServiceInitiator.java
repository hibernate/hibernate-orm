/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry;

import java.util.Map;

import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Contract for an initiator of services that target the standard {@link org.hibernate.service.ServiceRegistry}.
 *
 * @param <R> The type of the service initiated.
 *
 * @author Steve Ebersole
 */
public interface StandardServiceInitiator<R extends Service> extends ServiceInitiator<R> {
	/**
	 * Initiates the managed service.
	 *
	 * @param configurationValues The configuration values in effect
	 * @param registry The service registry.  Can be used to locate services needed to fulfill initiation.
	 *
	 * @return The initiated service.
	 */
	public R initiateService(Map configurationValues, ServiceRegistryImplementor registry);
}
