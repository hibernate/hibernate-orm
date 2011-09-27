/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.testing;

import java.util.Map;

import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.internal.StandardServiceRegistryImpl;

/**
 * @author Steve Ebersole
 */
public class ServiceRegistryBuilder {
	public static StandardServiceRegistryImpl buildServiceRegistry() {
		return buildServiceRegistry( Environment.getProperties() );
	}

	public static StandardServiceRegistryImpl buildServiceRegistry(Map serviceRegistryConfig) {
		return (StandardServiceRegistryImpl) new org.hibernate.service.ServiceRegistryBuilder()
				.applySettings( serviceRegistryConfig )
				.buildServiceRegistry();
	}

	public static void destroy(ServiceRegistry serviceRegistry) {
		( (StandardServiceRegistryImpl) serviceRegistry ).destroy();
	}
}
