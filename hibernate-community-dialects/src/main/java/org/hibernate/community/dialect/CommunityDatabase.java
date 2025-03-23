/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.Database;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * A list of community maintained relational database systems for which Hibernate can resolve a {@link Dialect}.
 *
 * However, Hibernate can work with other database systems that are not listed by the {@link Database}
 * enumeration, as long as a {@link Dialect} implementation class is provided via the {@code hibernate.dialect}
 * configuration property.
 *
 * @author Christian Beikov
 */
public enum CommunityDatabase {

	SQLITE {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new SQLiteDialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.startsWith( "SQLite" );
		}
	},

	INGRES {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new IngresDialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.toLowerCase().startsWith( "ingres" );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "com.ingres.jdbc.IngresDriver";
		}
	},

	INFORMIX {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new InformixDialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			//usually "Informix Dynamic Server"
			return databaseName.toLowerCase().startsWith( "informix" );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "com.informix.jdbc.IfxDriver" ;
		}
		@Override
		public String getUrlPrefix() {
			return "jdbc:informix-";
		}
	},

	FIREBIRD {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new FirebirdDialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.startsWith( "Firebird" );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "org.firebirdsql.jdbc.FBDriver";
		}
		@Override
		public String getUrlPrefix() {
			// Jaybird 4 and higher support jdbc:firebird: and jdbc:firebirdsql: as JDBC protocol
			return "jdbc:firebird";
		}
	},

	CACHE {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new CacheDialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.startsWith( "Cache" );
		}
	},

	CUBRID {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new CUBRIDDialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return "CUBRID".equalsIgnoreCase( databaseName );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "cubrid.jdbc.driver.CUBRIDDriver";
		}
	},

	ALTIBASE {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new AltibaseDialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return "Altibase".equalsIgnoreCase( databaseName );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "Altibase.jdbc.driver.AltibaseDriver";
		}
	},

	MIMER {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new MimerSQLDialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.startsWith( "Mimer SQL" );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "com.mimer.jdbc.Driver";
		}
	},

	MAXDB {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new MaxDBDialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.toLowerCase().startsWith( "sap db" )
					|| databaseName.toLowerCase().startsWith( "maxdb" );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return  "com.sap.dbtech.jdbc.DriverSapDB";
		}
		@Override
		public String getUrlPrefix() {
			return "jdbc:sapdb:";
		}
	},

	SYBASE_ANYWHERE {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();
			if ( isASA( databaseName ) ) {
				return new SybaseAnywhereDialect( info );
			}
			return null;
		}
		private boolean isASA(String databaseName) {
			return databaseName.startsWith( "Adaptive Server Anywhere" )
					|| "SQL Anywhere".equals( databaseName );
		}
		@Override
		public boolean productNameMatches(String productName) {
			return isASA( productName );
		}
		@Override
		public boolean matchesUrl(String jdbcUrl) {
			return jdbcUrl.startsWith( "jdbc:sybase:" )
					|| jdbcUrl.startsWith( "jdbc:sqlanywhere:" );
		}
	},

	TIMESTEN {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new TimesTenDialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.toLowerCase().startsWith( "timesten" );
		}
	},

	SINGLESTORE {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new SingleStoreDialect( info );
		}

		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.toLowerCase().startsWith( "singlestore" );
		}

		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "com.singlestore.jdbc.Driver";
		}
	},

	DERBY {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new DerbyDialect( info );
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return "Apache Derby".equals( databaseName );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return jdbcUrl.startsWith( "jdbc:derby://" )
					? "org.apache.derby.jdbc.ClientDriver"
					: "org.apache.derby.jdbc.EmbeddedDriver";
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
