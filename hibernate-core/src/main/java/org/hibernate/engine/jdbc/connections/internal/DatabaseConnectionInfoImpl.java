/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.engine.jdbc.connections.internal;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;
import org.hibernate.internal.util.StringHelper;

import static org.hibernate.dialect.SimpleDatabaseVersion.ZERO_VERSION;
import static org.hibernate.dialect.SimpleDatabaseVersion.NO_VERSION;

/**
 * @author Jan Schatteman
 */
public class DatabaseConnectionInfoImpl implements DatabaseConnectionInfo {

	// Means either the value was not explicitly set, or simply not offered by the connection provider
	public static final String DEFAULT = "undefined/unknown";

	protected String dbUrl = DEFAULT;
	protected String dbDriverName = DEFAULT;
	protected DatabaseVersion dbVersion = ZERO_VERSION;
	protected String dbAutoCommitMode = DEFAULT;
	protected String dbIsolationLevel = DEFAULT;
	protected String dbMinPoolSize = DEFAULT;
	protected String dbMaxPoolSize = DEFAULT;

	public DatabaseConnectionInfoImpl() {
	}

	@Override
	public DatabaseConnectionInfo setDBUrl(String dbUrl) {
		this.dbUrl = dbUrl;
		return this;
	}

	@Override
	public DatabaseConnectionInfo setDBDriverName(String dbDriverName) {
		if ( checkValidString(dbDriverName) && isDefaultStringValue(this.dbDriverName) ) {
			this.dbDriverName = dbDriverName;
		}
		return this;
	}

	@Override
	public DatabaseConnectionInfo setDBVersion(DatabaseVersion dbVersion) {
		if ( checkValidVersion(dbVersion) && ZERO_VERSION.equals(this.dbVersion) ) {
			this.dbVersion = dbVersion;
		}
		return this;
	}

	@Override
	public DatabaseConnectionInfo setDBAutoCommitMode(String dbAutoCommitMode) {
		if ( checkValidString(dbAutoCommitMode) && isDefaultStringValue(this.dbAutoCommitMode) ) {
			this.dbAutoCommitMode = dbAutoCommitMode;
		}
		return this;
	}

	@Override
	public DatabaseConnectionInfo setDBIsolationLevel(String dbIsolationLevel) {
		if ( checkValidString(dbIsolationLevel) && isDefaultStringValue(this.dbIsolationLevel) ) {
			this.dbIsolationLevel = dbIsolationLevel;
		}
		return this;
	}

	@Override
	public DatabaseConnectionInfo setDBMinPoolSize(String minPoolSize) {
		if ( checkValidInteger(minPoolSize) ) {
			this.dbMinPoolSize = minPoolSize;
		}
		return this;
	}

	@Override
	public DatabaseConnectionInfo setDBMaxPoolSize(String maxPoolSize) {
		if ( checkValidInteger(maxPoolSize) ) {
			this.dbMaxPoolSize = maxPoolSize;
		}
		return this;
	}

	private boolean checkValidInteger(String integerString) {
		try {
			return checkValidString( integerString ) && Integer.parseInt( integerString, 10 ) >= 0;
		}
		catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean checkValidString(String value) {
		return !( StringHelper.isBlank( value ) || "null".equalsIgnoreCase( value ) );
	}

	private boolean checkValidVersion(DatabaseVersion version) {
		return version != null && !( version.isSame(ZERO_VERSION) || version.isSame(NO_VERSION) );
	}

	private boolean isDefaultStringValue(String value) {
		return DEFAULT.equalsIgnoreCase( value );
	}

	@Override
	public String getDBInfoAsString() {
		StringBuilder sb = new StringBuilder();
		sb.append( "\tDatabase JDBC URL [" ).append( dbUrl ).append(']');
		sb.append(sb.length() > 0 ? "\n\t" : "" ).append( "Database driver: " ).append( dbDriverName );
		sb.append(sb.length() > 0 ? "\n\t" : "" ).append( "Database version: " ).append( dbVersion );
		sb.append(sb.length() > 0 ? "\n\t" : "" ).append( "Autocommit mode: " ).append( dbAutoCommitMode );
		sb.append(sb.length() > 0 ? "\n\t" : "" ).append( "Isolation level: " ).append( dbIsolationLevel );
		sb.append(sb.length() > 0 ? "\n\t" : "" ).append( "Minimum pool size: " ).append( dbMinPoolSize );
		sb.append(sb.length() > 0 ? "\n\t" : "" ).append( "Maximum pool size: " ).append( dbMaxPoolSize );
		return sb.toString();
	}
}
