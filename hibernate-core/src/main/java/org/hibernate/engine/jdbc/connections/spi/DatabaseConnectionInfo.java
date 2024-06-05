/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.engine.jdbc.connections.spi;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.SimpleDatabaseVersion;

/**
 * @author Jan Schatteman
 */
public interface DatabaseConnectionInfo {
	public static final String NOT_PROVIDED = "Not Provided";
	default String getDBUrl() {
		return NOT_PROVIDED;
	}

	default String getDBDriverName() {
		return NOT_PROVIDED;
	}

	default DatabaseVersion getDBVersion() {
		return SimpleDatabaseVersion.ZERO_VERSION;
	}
}
