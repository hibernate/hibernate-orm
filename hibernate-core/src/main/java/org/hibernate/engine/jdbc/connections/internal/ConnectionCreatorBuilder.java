/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
