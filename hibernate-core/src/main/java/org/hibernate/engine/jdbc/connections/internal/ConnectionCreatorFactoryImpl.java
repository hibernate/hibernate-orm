/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Driver;
import java.util.Map;
import java.util.Properties;

import org.hibernate.service.ServiceRegistry;

/**
 * The default factory for {@link ConnectionCreator} instances.
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
			ServiceRegistry serviceRegistry,
			String url,
			Properties connectionProps,
			Boolean autoCommit,
			Integer isolation,
			String initSql,
			Map<String, Object> configurationValues) {
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
