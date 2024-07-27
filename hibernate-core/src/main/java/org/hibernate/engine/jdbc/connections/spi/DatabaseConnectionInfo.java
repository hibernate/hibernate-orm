/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.engine.jdbc.connections.spi;

import org.hibernate.dialect.DatabaseVersion;

/**
 * @author Jan Schatteman
 */
public interface DatabaseConnectionInfo {
	DatabaseConnectionInfo setDBUrl(String dbUrl);

	DatabaseConnectionInfo setDBDriverName(String dbDriverName);

	DatabaseConnectionInfo setDBVersion(DatabaseVersion dbVersion);

	DatabaseConnectionInfo setDBAutoCommitMode(String dbAutoCommitMode);

	DatabaseConnectionInfo setDBIsolationLevel(String dbIsolationLevel);

	DatabaseConnectionInfo setDBMinPoolSize(String minPoolSize);

	DatabaseConnectionInfo setDBMaxPoolSize(String maxPoolSize);

	String getDBInfoAsString();
}
