/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.connections.internal;

import java.sql.Driver;
import java.util.Map;
import java.util.Properties;

import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * A factory for {@link ConnectionCreator}.
 *
 * @author Christian Beikov
 */
interface ConnectionCreatorFactory {

	ConnectionCreator create(
			Driver driver,
			ServiceRegistryImplementor serviceRegistry,
			String url,
			Properties connectionProps,
			Boolean autocommit,
			Integer isolation,
			String initSql,
			Map<String, Object> configurationValues);

}
