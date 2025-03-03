/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import static org.hibernate.cfg.AvailableSettings.FORMAT_SQL;
import static org.hibernate.cfg.AvailableSettings.HIGHLIGHT_SQL;
import static org.hibernate.cfg.AvailableSettings.LOG_SLOW_QUERY;
import static org.hibernate.cfg.AvailableSettings.SHOW_SQL;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.internal.util.config.ConfigurationHelper.getLong;

/**
 * The {@link SqlStatementLogger} is accessible via {@link org.hibernate.engine.jdbc.spi.JdbcServices},
 * but during service initialization, it might be needed before the {@code JdbcServices} service is
 * initialized.
 */
public class SqlStatementLoggerInitiator implements StandardServiceInitiator<SqlStatementLogger> {

	// this deprecated property name never respected our conventions
	private static final String OLD_LOG_SLOW_QUERY = "hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS";

	public static final SqlStatementLoggerInitiator INSTANCE = new SqlStatementLoggerInitiator();

	@Override
	public SqlStatementLogger initiateService(Map<String, Object> configValues, ServiceRegistryImplementor registry) {
		final boolean showSQL = getBoolean( SHOW_SQL, configValues );
		final boolean formatSQL = getBoolean( FORMAT_SQL, configValues );
		final boolean highlightSQL = getBoolean( HIGHLIGHT_SQL, configValues );

		long logSlowQuery = getLong( LOG_SLOW_QUERY, configValues, -2 );
		if ( logSlowQuery == -2 ) {
			logSlowQuery = getLong( OLD_LOG_SLOW_QUERY, configValues, 0 );
		}

		return new SqlStatementLogger( showSQL, formatSQL, highlightSQL, logSlowQuery );
	}

	@Override
	public Class<SqlStatementLogger> getServiceInitiated() {
		return SqlStatementLogger.class;
	}
}
