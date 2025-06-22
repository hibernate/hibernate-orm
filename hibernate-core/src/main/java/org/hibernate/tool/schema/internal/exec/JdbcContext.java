/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal.exec;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.service.ServiceRegistry;

/**
 * Access to JDBC context for schema tooling activity.
 *
 * @author Steve Ebersole
 */
public interface JdbcContext {
	JdbcConnectionAccess getJdbcConnectionAccess();
	Dialect getDialect();
	SqlStatementLogger getSqlStatementLogger();
	SqlExceptionHelper getSqlExceptionHelper();
	ServiceRegistry getServiceRegistry();
}
