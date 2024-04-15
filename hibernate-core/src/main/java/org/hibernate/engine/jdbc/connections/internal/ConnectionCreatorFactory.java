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
 * A factory for {@link ConnectionCreator}.
 *
 * @author Christian Beikov
 */
interface ConnectionCreatorFactory {

	public ConnectionCreator create(
			Driver driver,
			ServiceRegistryImplementor serviceRegistry,
			String url,
			Properties connectionProps,
			Boolean autocommit,
			Integer isolation,
			String initSql,
			Map<Object, Object> configurationValues);

}
