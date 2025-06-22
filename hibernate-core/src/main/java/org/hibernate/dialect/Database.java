/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.engine.jdbc.dialect.spi.BasicSQLExceptionConverter;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.internal.util.config.ConfigurationHelper;

import static org.hibernate.cfg.DialectSpecificSettings.COCKROACH_VERSION_STRING;

/**
 * A list of relational database systems for which Hibernate can resolve a {@link Dialect}.
 *
 * However, Hibernate can work with other database systems that are not listed by the {@link Database}
 * enumeration, as long as a {@link Dialect} implementation class is provided via the {@code hibernate.dialect}
 * configuration property.
 *
 * @author Vlad Mihalcea
 */
public enum Database {

	DB2 {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			String databaseVersion = info.getDatabaseVersion();
			if ( databaseVersion != null ) {
				//See https://www.ibm.com/support/knowledgecenter/SSEPEK_12.0.0/java/src/tpc/imjcc_c0053013.html
				switch ( databaseVersion.substring( 0, 3 ) ) {
					case "SQL": {
						// Linux, UNIX, Windows
						return new DB2Dialect( info );
					}
					case "DSN": {
						// z/OS
						return new DB2zDialect( info );
					}
					case "QSQ": {
						// i
						return new DB2iDialect( info );
					}
				}
			}

			return new DB2Dialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.startsWith( "DB2" );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "com.ibm.db2.jcc.DB2Driver";
		}
	},

	ENTERPRISEDB {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new PostgresPlusDialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return "EnterpriseDB".equals( databaseName );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "com.edb.Driver";
		}
		@Override
		public String getUrlPrefix() {
			return "jdbc:edb:";
		}
	},

	H2 {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new H2Dialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return "H2".equals( databaseName );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "org.h2.Driver";
		}
	},

	HSQL {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new HSQLDialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return "HSQL Database Engine".equals( databaseName );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "org.hsqldb.jdbc.JDBCDriver";
		}
		@Override
		public String getUrlPrefix() {
			return "jdbc:hsqldb:";
		}
	},

	HANA {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new HANADialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return "HDB".equals( databaseName );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "com.sap.db.jdbc.Driver";
		}
		@Override
		public String getUrlPrefix() {
			return "jdbc:sap:";
		}
	},

	MARIADB {
		@Override
		public boolean matchesResolutionInfo(DialectResolutionInfo info) {
			if ( productNameMatches( info.getDatabaseName() ) ) {
				return true;
			}
			else {
				//in case the product name has been set to MySQL
				String driverName = info.getDriverName();
				return driverName != null && driverName.startsWith( "MariaDB" );
			}
		}
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new MariaDBDialect( info );
		}
		@Override
		public boolean productNameMatches(String productName) {
			return "MariaDB".equals( productName );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "org.mariadb.jdbc.Driver";
		}
	},

	MYSQL {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new MySQLDialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return "MySQL".equals( databaseName );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "com.mysql.cj.jdbc.Driver";
		}
	},

	ORACLE {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new OracleDialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return "Oracle".equals( databaseName );
		}
		/*@Override
		public String getDriverClassName() {
			return "oracle.jdbc.OracleDriver";
		}*/
	},

	POSTGRESQL {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			final String version = getVersion( info );
			if ( version.startsWith( "Cockroach" ) ) {
				return new CockroachDialect( info, version );
			}
			return new PostgreSQLDialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return "PostgreSQL".equals( databaseName );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "org.postgresql.Driver";
		}
		private String getVersion(DialectResolutionInfo info) {
			final DatabaseMetaData databaseMetaData = info.getDatabaseMetadata();
			if ( databaseMetaData != null ) {
				try ( Statement statement = databaseMetaData.getConnection().createStatement() ) {
					final ResultSet rs = statement.executeQuery( "select version()" );
					if ( rs.next() ) {
						return rs.getString( 1 );
					}
				}
				catch (SQLException e) {
					throw BasicSQLExceptionConverter.INSTANCE.convert( e );
				}
			}

			// default to the dialect-specific configuration setting
			return ConfigurationHelper.getString( COCKROACH_VERSION_STRING, info.getConfigurationValues(), "" );
		}
	},

	SPANNER {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new SpannerDialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.startsWith( "Google Cloud Spanner" );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "com.google.cloud.spanner.jdbc.JdbcDriver";
		}
	},

	SQLSERVER {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new SQLServerDialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.startsWith( "Microsoft SQL Server" );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
		}
	},

	SYBASE {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();
			if ( isASE( databaseName ) ) {
				return new SybaseASEDialect( info );
			}
			return null;
		}
		private boolean isASE(String databaseName) {
			return "Sybase SQL Server".equals( databaseName )
				|| "Adaptive Server Enterprise".equals( databaseName )
					|| "ASE".equals( databaseName );
		}
		@Override
		public boolean productNameMatches(String productName) {
			return isASE( productName );
		}
		@Override
		public boolean matchesUrl(String jdbcUrl) {
			return jdbcUrl.startsWith( "jdbc:sybase:" )
					|| jdbcUrl.startsWith( "jdbc:sqlanywhere:" );
		}
	};

	/**
	 * Does this database match the given metadata?
	 */
	public boolean matchesResolutionInfo(DialectResolutionInfo info) {
		return productNameMatches( info.getDatabaseName() );
	}

	/**
	 * Does this database have the given product name?
	 */
	public abstract boolean productNameMatches(String productName);

	/**
	 * Create a {@link Dialect} for the given metadata.
	 */
	public abstract Dialect createDialect(DialectResolutionInfo info);

	/**
	 * Get the name of the JDBC driver class for this database,
	 * or null if we're not too sure what it should be.
	 */
	public String getDriverClassName(String jdbcUrl) {
		return null;
	}

	/**
	 * Get the JDBC URL prefix used by this database.
	 */
	public String getUrlPrefix() {
		return "jdbc:" + toString().toLowerCase() + ":";
	}

	/**
	 * Does the given JDBC URL connect to this database?
	 */
	public boolean matchesUrl(String jdbcUrl) {
		return jdbcUrl.toLowerCase().startsWith( getUrlPrefix() );
	}

}
