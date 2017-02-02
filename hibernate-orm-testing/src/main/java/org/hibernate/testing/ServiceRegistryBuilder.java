/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Steve Ebersole
 */
public final class ServiceRegistryBuilder {
	private ServiceRegistryBuilder() {
	}

	public static StandardServiceRegistryImpl buildServiceRegistry() {
		return buildServiceRegistry( Environment.getProperties() );
	}

	public static StandardServiceRegistryImpl buildServiceRegistry(Map serviceRegistryConfig) {
		return (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder()
				.applySettings( serviceRegistryConfig )
				.build();
	}

	public static void destroy(ServiceRegistry serviceRegistry) {
		( (StandardServiceRegistryImpl) serviceRegistry ).destroy();
	}
}
