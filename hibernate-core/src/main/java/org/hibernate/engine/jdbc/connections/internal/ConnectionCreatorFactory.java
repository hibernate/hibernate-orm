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
 * A factory for {@link ConnectionCreator}.
 *
 * @author Christian Beikov
 */
public interface ConnectionCreatorFactory {

	ConnectionCreator create(
			Driver driver,
			ServiceRegistry serviceRegistry,
			String url,
			Properties connectionProps,
			Boolean autocommit,
			Integer isolation,
			String initSql,
			Map<String, Object> configurationValues);

}
