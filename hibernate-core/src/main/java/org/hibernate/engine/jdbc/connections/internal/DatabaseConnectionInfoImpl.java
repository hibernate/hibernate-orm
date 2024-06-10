/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.engine.jdbc.connections.internal;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.SimpleDatabaseVersion;
import org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo;

/**
 * @author Jan Schatteman
 */
public class DatabaseConnectionInfoImpl implements DatabaseConnectionInfo {

	private String url;
	private String driverName;
	private DatabaseVersion dbVersion;

	public DatabaseConnectionInfoImpl() {
	}

	public DatabaseConnectionInfoImpl(String url) {
		this(url, null);
	}

	public DatabaseConnectionInfoImpl(String url, String driverName) {
		this(url, driverName, SimpleDatabaseVersion.ZERO_VERSION);
	}

	public DatabaseConnectionInfoImpl(String url, String driverName, DatabaseVersion dbVersion) {
		this.url = url;
		this.driverName = driverName;
		this.dbVersion = dbVersion;
	}

	public DatabaseConnectionInfoImpl(DatabaseConnectionInfo dci) {
		this( dci.getDBUrl(), dci.getDBDriverName(), dci.getDBVersion() );
	}

	@Override
	public String getDBUrl() {
		return url != null ? url : DatabaseConnectionInfo.super.getDBUrl();
	}

	@Override
	public String getDBDriverName() {
		return driverName != null ? driverName : DatabaseConnectionInfo.super.getDBDriverName();
	}

	@Override
	public DatabaseVersion getDBVersion() {
		return dbVersion;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setDriverName(String driverName) {
		this.driverName = driverName;
	}

	public void setDbVersion(DatabaseVersion dbVersion) {
		this.dbVersion = dbVersion;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if ( getDBUrl() != null ) {
			sb.append( "\tDatabase JDBC URL [" ).append( getDBUrl() ).append(']');
		}
		if ( getDBDriverName() != null ) {
			sb.append(sb.length() > 0 ? "\n\t" : "" ).append( "Database driver: " ).append( getDBDriverName() );
		}
		if ( getDBVersion() != null ) {
			sb.append(sb.length() > 0 ? "\n\t" : "" ).append( "Database version: " ).append( getDBVersion() );
		}
		return sb.toString();
	}
}
