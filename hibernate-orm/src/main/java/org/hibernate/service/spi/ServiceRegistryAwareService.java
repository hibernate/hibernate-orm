/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service.spi;

/**
 * Allows services to be injected with the {@link org.hibernate.service.ServiceRegistry} during configuration phase.
 *
 * @author Steve Ebersole
 */
public interface ServiceRegistryAwareService {
	/**
	 * Callback to inject the registry.
	 *
	 * @param serviceRegistry The registry
	 */
	public void injectServices(ServiceRegistryImplementor serviceRegistry);
}
