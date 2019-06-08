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
 * However, Hibernate can work with other database systems that are not listed by the {@link Database} enum,
 * as long as a {@link Dialect} implementation class is provided via the {@code hibernate.dialect} configuration property.
 *
 * @author Vlad Mihalcea
 */
public enum Database {

	CACHE {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( databaseName.startsWith( "Cache" ) ) {
				return new CacheDialect();
			}

			return null;
		}
	},
	CUBRID {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "CUBRID".equalsIgnoreCase( databaseName ) ) {
				return new CUBRIDDialect();
			}

			return null;
		}
	},
	DB2 {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "DB2 UDB for AS/400".equals( databaseName ) ) {
				return new DB2400Dialect();
			}

			if ( databaseName.startsWith( "DB2" ) ) {

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
				}
			}

			return null;
		}
	},
	DERBY {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "Apache Derby".equals( databaseName ) ) {
				return new DerbyDialect(info);
			}

			return null;
		}
	},
	ENTERPRISEDB {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "EnterpriseDB".equals( databaseName ) ) {
				return new PostgresPlusDialect();
			}

			return null;
		}
	},
	FIREBIRD {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( databaseName.startsWith( "Firebird" ) ) {
				return new FirebirdDialect();
			}

			return null;
		}
	},
	FRONTBASE {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( databaseName.startsWith( "FrontBase" ) ) {
				return new FrontBaseDialect();
			}

			return null;
		}
	},
	H2 {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "H2".equals( databaseName ) ) {
				return new H2Dialect(info);
			}

			return null;
		}
	},
	HANA {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "HDB".equals( databaseName ) ) {
				// SAP recommends defaulting to column store.
				return new HANAColumnStoreDialect();
			}

			return null;
		}
	},
	HSQL {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "HSQL Database Engine".equals( databaseName ) ) {
				return new HSQLDialect(info);
			}

			return null;
		}
	},
	INFORMIX {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName().toLowerCase();

			//usually "Informix Dynamic Server"
			if ( databaseName.startsWith("informix") ) {
				return new InformixDialect(info);
			}

			return null;
		}
	},
	INGRES {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName().toLowerCase();

			if ( databaseName.startsWith("ingres") ) {
				return new IngresDialect(info);
			}
			return null;
		}
	},
	INTERBASE {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName().toLowerCase();

			if ( databaseName.startsWith("interbase") ) {
				return new InterbaseDialect();
			}
			return null;
		}
	},
	MARIADB {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			String driverName = info.getDriverName();

			if ( driverName != null && driverName.startsWith( "MariaDB" ) ) {
				return new MariaDBDialect(info);
			}

			return null;
		}
	},
	MAXDB {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName().toLowerCase();

			if ( databaseName.startsWith("sap db") || databaseName.startsWith("maxdb") ) {
				return new SAPDBDialect();
			}

			return null;
		}
	},
	MCKOI {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName().toLowerCase();

			if ( databaseName.startsWith("mckoi") ) {
				return new MckoiDialect();
			}

			return null;
		}
	},
	MIMERSQL {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "Mimer SQL Experience".equals( databaseName ) ) {
				return new MimerSQLDialect();
			}

			return null;
		}
	},
	MYSQL {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "MySQL".equals( databaseName ) ) {
				return new MySQLDialect(info);
			}

			return null;
		}
	},
	ORACLE {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "Oracle".equals( databaseName ) ) {
				return new OracleDialect(info);
			}

			return null;
		}
	},
	POINTBASE {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName().toLowerCase();

			if ( databaseName.startsWith("pointbase") ) {
				return new PointbaseDialect();
			}
			return null;
		}
	},
	POSTGRESQL {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "PostgreSQL".equals( databaseName ) ) {
				return new PostgreSQLDialect(info);
			}

			return null;
		}
	},
	PROGRESS {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName().toLowerCase();

			if ( databaseName.startsWith("progress") || databaseName.startsWith("openedge") ) {
				return new ProgressDialect();
			}

			return null;
		}
	},
	SQLSERVER {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( databaseName.startsWith( "Microsoft SQL Server" ) ) {
				return new SQLServerDialect(info);
			}

			return null;
		}
	},
	SYBASE {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "Sybase SQL Server".equals( databaseName ) || "Adaptive Server Enterprise".equals( databaseName )
					|| databaseName.toLowerCase().startsWith( "ase" ) ) {
				return new SybaseASEDialect(info);
			}

			if ( databaseName.startsWith( "Adaptive Server Anywhere" ) || "SQL Anywhere".equals( databaseName ) ) {
				return new SybaseAnywhereDialect();
			}

			return null;
		}
	},
	TERADATA {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "Teradata".equals( databaseName ) ) {
				return new TeradataDialect(info);
			}
			return null;
		}
	},
	TIMESTEN {
		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName().toLowerCase();

			if ( databaseName.startsWith("timesten") ) {
				return new TimesTenDialect();
			}
			return null;
		}
	};

	public abstract Dialect resolveDialect(DialectResolutionInfo info);

}
