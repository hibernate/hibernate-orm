/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
