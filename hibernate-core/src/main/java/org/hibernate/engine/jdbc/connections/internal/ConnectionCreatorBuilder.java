/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Driver;
import java.util.Properties;

import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * A builder for ConnectionCreator instances
 *
 * @author Steve Ebersole
 */
public class ConnectionCreatorBuilder {
	private final ServiceRegistryImplementor serviceRegistry;

	private Driver driver;

	private String url;
	private Properties connectionProps;

	private boolean autoCommit;
	private Integer isolation;

	public ConnectionCreatorBuilder(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void setDriver(Driver driver) {
		this.driver = driver;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setConnectionProps(Properties connectionProps) {
		this.connectionProps = connectionProps;
	}

	public void setAutoCommit(boolean autoCommit) {
		this.autoCommit = autoCommit;
	}

	public void setIsolation(Integer isolation) {
		this.isolation = isolation;
	}

	public ConnectionCreator build() {
		if ( driver == null ) {
			return new DriverManagerConnectionCreator( serviceRegistry, url, connectionProps, autoCommit, isolation );
		}
		else {
			return new DriverConnectionCreator( driver, serviceRegistry, url, connectionProps, autoCommit, isolation );
		}
	}
}
