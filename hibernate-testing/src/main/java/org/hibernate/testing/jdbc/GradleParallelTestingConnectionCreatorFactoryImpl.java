/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.jdbc;

import org.hibernate.engine.jdbc.connections.internal.ConnectionCreator;
import org.hibernate.engine.jdbc.connections.internal.ConnectionCreatorFactory;
import org.hibernate.engine.jdbc.connections.internal.DriverConnectionCreator;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionCreator;
import org.hibernate.service.ServiceRegistry;

import java.sql.Driver;
import java.util.Map;
import java.util.Properties;

import static org.hibernate.testing.jdbc.GradleParallelTestingResolver.*;

/**
 * A factory for {@link ConnectionCreator} instances that can be used for Gradle Parallel Testing.
 *
 * @author Loïc Lefèvre
 */
public class GradleParallelTestingConnectionCreatorFactoryImpl implements ConnectionCreatorFactory {

	public GradleParallelTestingConnectionCreatorFactoryImpl() {
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

		// now resolve JDBC Username if needed for Gradle Parallel Testing
		url = resolveUrl( url );
		resolve( connectionProps );

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
