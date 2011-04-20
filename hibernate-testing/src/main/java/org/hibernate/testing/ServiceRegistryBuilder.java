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

import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.internal.BasicServiceRegistryImpl;

import java.util.Map;
import java.util.Properties;

/**
 * @author Steve Ebersole
 */
public class ServiceRegistryBuilder {
	public static BasicServiceRegistryImpl buildServiceRegistry() {
		return buildServiceRegistry( Environment.getProperties() );
	}

	public static BasicServiceRegistryImpl buildServiceRegistry(Map serviceRegistryConfig) {
		Properties properties = new Properties();
		properties.putAll( serviceRegistryConfig );
		Environment.verifyProperties( properties );
		ConfigurationHelper.resolvePlaceHolders( properties );
		return (BasicServiceRegistryImpl) new org.hibernate.service.ServiceRegistryBuilder( properties ).buildServiceRegistry();
	}

	public static void destroy(ServiceRegistry serviceRegistry) {
		( (BasicServiceRegistryImpl) serviceRegistry ).destroy();
	}
}
