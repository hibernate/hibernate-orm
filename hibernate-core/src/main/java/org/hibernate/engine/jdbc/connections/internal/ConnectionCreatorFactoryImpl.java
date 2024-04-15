/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Driver;
import java.util.Map;
import java.util.Properties;

import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * The default factory for ConnectionCreator instances
 *
 * @author Christian Beikov
 */
public class ConnectionCreatorFactoryImpl implements ConnectionCreatorFactory {

	public static final ConnectionCreatorFactory INSTANCE = new ConnectionCreatorFactoryImpl();

	private ConnectionCreatorFactoryImpl() {
	}

	@Override
	public ConnectionCreator create(
			Driver driver,
			ServiceRegistryImplementor serviceRegistry,
			String url,
			Properties connectionProps,
			Boolean autoCommit,
			Integer isolation,
			String initSql,
			Map<Object, Object> configurationValues) {
		if ( driver == null ) {
			return new DriverManagerConnectionCreator(
					serviceRegistry,
					url,
					connectionProps,
					autoCommit,
					isolation,
					initSql
			);
		}
		else {
			return new DriverConnectionCreator(
					driver,
					serviceRegistry,
					url,
					connectionProps,
					autoCommit,
					isolation,
					initSql
			);
		}
	}
}
