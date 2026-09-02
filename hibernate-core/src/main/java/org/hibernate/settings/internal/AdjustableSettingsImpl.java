/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.settings.internal;

import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.settings.AdjustableSettings;

/// Standard AdjustableSettings implementation.
///
/// @author Steve Ebersole
public class AdjustableSettingsImpl implements AdjustableSettings {
	private final SessionFactoryImplementor sessionFactory;

	public AdjustableSettingsImpl(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	private SqlStatementLogger getSqlStatementLogger() {
		return sessionFactory.getServiceRegistry().requireService( SqlStatementLogger.class );
	}

	@Override
	public void setShowSql(boolean newValue) {
		getSqlStatementLogger().setLogToStdout( newValue );
	}

	@Override
	public void setFormatLoggedSql(boolean newValue) {
		getSqlStatementLogger().setFormat( newValue );
	}

	@Override
	public void setSlowSqlLoggingThreshold(long newValue) {
		getSqlStatementLogger().setLogSlowQuery( newValue );
	}

	private SqlExceptionHelper getSqlExceptionHelper() {
		return sessionFactory.getServiceRegistry().requireService( JdbcEnvironment.class ).getSqlExceptionHelper();
	}

	@Override
	public void setLogJdbcWarnings(boolean newValue) {
		getSqlExceptionHelper().setLogWarnings( newValue );
	}

	@Override
	public void setLogJdbcErrors(boolean newValue) {
		getSqlExceptionHelper().setLogErrors( newValue );
	}
}
