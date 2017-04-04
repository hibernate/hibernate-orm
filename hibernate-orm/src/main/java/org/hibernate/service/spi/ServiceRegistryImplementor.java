/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service.spi;

import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;

/**
 * Additional integration contracts for a service registry.
 *
 * @author Steve Ebersole
 */
public interface ServiceRegistryImplementor extends ServiceRegistry {
	/**
	 * Locate the binding for the given role.  Should, generally speaking, look into parent registry if one.
	 *
	 * @param serviceRole The service role for which to locate a binding.
	 * @param <R> generic return type.
	 *
	 * @return The located binding; may be {@code null}
	 */
	public <R extends Service> ServiceBinding<R> locateServiceBinding(Class<R> serviceRole);

	/**
	 * Release resources
	 */
	public void destroy();

	/**
	 * When a registry is created with a parent, the parent is notified of the child
	 * via this callback.
	 */
	public void registerChild(ServiceRegistryImplementor child);

	/**
	 * When a registry is created with a parent, the parent is notified of the child
	 * via this callback.
	 */
	public void deRegisterChild(ServiceRegistryImplementor child);
}
