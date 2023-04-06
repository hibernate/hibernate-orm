/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * The {@link SqlStatementLogger} is accessible via {@link org.hibernate.engine.jdbc.spi.JdbcServices},
 * but during service initialization, it might be needed before the {@code JdbcServices} service is initialized.
 *
 * For Hibernate Reactive
 */
public class SqlStatementLoggerInitiator implements StandardServiceInitiator<SqlStatementLogger> {

	public static final SqlStatementLoggerInitiator INSTANCE = new SqlStatementLoggerInitiator();

	@Override
	public SqlStatementLogger initiateService(Map<String, Object> configValues, ServiceRegistryImplementor registry) {
		final boolean showSQL = ConfigurationHelper.getBoolean( Environment.SHOW_SQL, configValues, false );
		final boolean formatSQL = ConfigurationHelper.getBoolean( Environment.FORMAT_SQL, configValues, false );
		final boolean highlightSQL = ConfigurationHelper.getBoolean( Environment.HIGHLIGHT_SQL, configValues, false );
		final long logSlowQuery = ConfigurationHelper.getLong( Environment.LOG_SLOW_QUERY, configValues, 0 );

		return new SqlStatementLogger( showSQL, formatSQL, highlightSQL, logSlowQuery );
	}

	@Override
	public Class<SqlStatementLogger> getServiceInitiated() {
		return SqlStatementLogger.class;
	}
}
