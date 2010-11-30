/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.cfg.Environment;
import org.hibernate.cfg.internal.ServicesRegistryBootstrap;
import org.hibernate.engine.jdbc.internal.JdbcServicesImpl;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.internal.ServicesRegistryImpl;
import org.hibernate.service.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.service.spi.ServicesRegistry;

/**
 * @author Gail Badner
 */
public class ServiceRegistryHolder {
	private final ServicesRegistryImpl serviceRegistry;
	private final Properties properties;

	public ServiceRegistryHolder(Map props) {
		properties = new Properties();
		properties.putAll( props );
		Environment.verifyProperties( properties );
		ConfigurationHelper.resolvePlaceHolders( properties );
		serviceRegistry = new ServicesRegistryBootstrap().initiateServicesRegistry( properties );
		properties.putAll( serviceRegistry.getService( JdbcServices.class ).getDialect().getDefaultProperties() );
	}

	public Properties getProperties() {
		return properties;
	}

	public ServicesRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public JdbcServices getJdbcServices() {
		return serviceRegistry.getService( JdbcServices.class );
	}

	public JdbcServicesImpl getJdbcServicesImpl() {
		return ( JdbcServicesImpl ) getJdbcServices();
	}

	public ClassLoaderService getClassLoaderService() {
		return serviceRegistry.getService( ClassLoaderService.class );
	}

	public void destroy() {
		if ( serviceRegistry != null ) {
			serviceRegistry.destroy();
		}
	}
}
