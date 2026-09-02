/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.settings;

import org.hibernate.Incubating;

/// Allows runtime adjustment of a discrete set of Hibernate settings.
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public interface AdjustableSettings {
	/// Toggle whether Hibernate should log SQL to [stdout][System#out].
	///
	/// @see org.hibernate.cfg.JdbcSettings#SHOW_SQL
	void setShowSql(boolean newValue);

	/// Toggle whether Hibernate should format SQL prior to logging/showing it.
	///
	/// @see org.hibernate.cfg.JdbcSettings#FORMAT_SQL
	void setFormatLoggedSql(boolean newValue);

	/// Specify a duration threshold, in milliseconds, beyond which Hibernate will
	/// consider a SQL execution "slow" and log it.  Any value less-than-or-equal-to
	/// zero will effectively disable this logging.
	///
	/// @see org.hibernate.cfg.JdbcSettings#LOG_SLOW_QUERY
	void setSlowSqlLoggingThreshold(long newValue);

	/// Toggles whether Hibernate should log any warnings it receives interacting
	/// with the JDBC driver.
	///
	/// @see org.hibernate.cfg.JdbcSettings#LOG_JDBC_WARNINGS
	void setLogJdbcWarnings(boolean newValue);

	/// Toggles whether Hibernate should log any errors it receives interacting
	/// with the JDBC driver.
	///
	/// @apiNote Generally these errors are converted into other types of exceptions and
	/// thrown; enabling this opts-in to log-and-throw behavior.
	///
	/// @see org.hibernate.cfg.JdbcSettings#LOG_JDBC_ERRORS
	void setLogJdbcErrors(boolean newValue);
}
