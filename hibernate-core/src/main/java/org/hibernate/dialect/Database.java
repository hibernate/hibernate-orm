/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

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

	CACHE {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new CacheDialect();
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.startsWith( "Cache" );
		}
	},

	CUBRID {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new CUBRIDDialect();
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

	DB2 {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
//			if ( "DB2 UDB for AS/400".equals( info.getDatabaseName() ) ) {
//				return new DB2400Dialect();
//			}
			String databaseVersion = info.getDatabaseVersion();
			if ( databaseVersion==null ) {
				return new DB2Dialect(info);
			}
			//See https://www.ibm.com/support/knowledgecenter/SSEPEK_12.0.0/java/src/tpc/imjcc_c0053013.html
			switch ( databaseVersion.substring(0,3) ) {
				case "SQL": // Linux, UNIX, Windows
					return new DB2Dialect(info);
				case "DSN": // z/OS
					return new DB2390Dialect(info);
				case "QSQ": // i
					return new DB2400Dialect();
				default:
					return null;
			}
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

	DERBY {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new DerbyDialect(info);
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return "Apache Derby".equals( databaseName );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return jdbcUrl.startsWith("jdbc:derby://")
					? "org.apache.derby.jdbc.ClientDriver"
					: "org.apache.derby.jdbc.EmbeddedDriver";
		}
	},

	ENTERPRISEDB {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new PostgresPlusDialect();
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

	FIREBIRD {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new FirebirdDialect(info);
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
			return "jdbc:firebirdsql:";
		}
	},

	FRONTBASE {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new FrontBaseDialect();
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.startsWith( "FrontBase" );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "com.frontbase.jdbc.FBJDriver";
		}
	},

	H2 {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new H2Dialect(info);
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

	HANA {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new HANAColumnStoreDialect();
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

	HSQL {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new HSQLDialect(info);
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

	INFORMIX {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new InformixDialect(info);
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			//usually "Informix Dynamic Server"
			return databaseName.toLowerCase().startsWith("informix");
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

	INGRES {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new IngresDialect(info);
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.toLowerCase().startsWith("ingres");
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "com.ingres.jdbc.IngresDriver";
		}
	},

	INTERBASE {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new InterbaseDialect();
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.toLowerCase().startsWith("interbase");
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "interbase.interclient.Driver";
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
				return driverName != null && driverName.startsWith("MariaDB");
			}
		}
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new MariaDBDialect(info);
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

	MAXDB {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new SAPDBDialect();
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.toLowerCase().startsWith("sap db")
				|| databaseName.toLowerCase().startsWith("maxdb");
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

	MCKOI {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new MckoiDialect();
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.toLowerCase().startsWith("mckoi");
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "com.mckoi.JDBCDriver";
		}
	},

	MIMER {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new MimerSQLDialect();
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.startsWith("Mimer SQL");
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "com.mimer.jdbc.Driver";
		}
	},

	MYSQL {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new MySQLDialect(info);
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
			return new OracleDialect(info);
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

	POINTBASE {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new PointbaseDialect();
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.toLowerCase().startsWith("pointbase");
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "com.pointbase.jdbc.jdbcUniversalDriver";
		}
	},

	POSTGRESQL {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new PostgreSQLDialect(info);
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return "PostgreSQL".equals( databaseName );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "org.postgresql.Driver";
		}
	},

	PROGRESS {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new ProgressDialect();
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.toLowerCase().startsWith("progress")
				|| databaseName.toLowerCase().startsWith("openedge");
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "com.ddtek.jdbc.openedge.OpenEdgeDriver";
		}
		@Override
		public String getUrlPrefix() {
			return "jdbc:datadirect:openedge:";
		}
	},

	SQLSERVER {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new SQLServerDialect(info);
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
				return new SybaseASEDialect(info);
			}
			if ( isASA( databaseName ) ) {
				return new SybaseAnywhereDialect();
			}
			return null; //impossible
		}
		private boolean isASA(String databaseName) {
			return databaseName.startsWith( "Adaptive Server Anywhere" )
				|| "SQL Anywhere".equals( databaseName );
		}
		private boolean isASE(String databaseName) {
			return "Sybase SQL Server".equals( databaseName )
				|| "Adaptive Server Enterprise".equals( databaseName );
		}
		@Override
		public boolean productNameMatches(String productName) {
			return isASA( productName ) || isASE( productName );
		}
		@Override
		public boolean matchesUrl(String jdbcUrl) {
			return jdbcUrl.startsWith("jdbc:sybase:")
				|| jdbcUrl.startsWith("jdbc:sqlanywhere:");
		}
	},

	TERADATA {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new TeradataDialect(info);
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return "Teradata".equals( databaseName );
		}
		@Override
		public String getDriverClassName(String jdbcUrl) {
			return "com.teradata.jdbc.TeraDriver";
		}
	},

	TIMESTEN {
		@Override
		public Dialect createDialect(DialectResolutionInfo info) {
			return new TimesTenDialect();
		}
		@Override
		public boolean productNameMatches(String databaseName) {
			return databaseName.toLowerCase().startsWith("timesten");
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
