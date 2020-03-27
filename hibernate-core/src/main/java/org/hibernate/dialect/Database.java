/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

/**
 * List all supported relational database systems.
 *
 * @author Vlad Mihalcea
 */
public enum Database {

	CACHE {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return Cache71Dialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			return null;
		}
	},
	CUBRID {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return CUBRIDDialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "CUBRID".equalsIgnoreCase( databaseName ) ) {
				return latestDialectInstance( this );
			}

			return null;
		}
	},
	DB2 {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return DB2400V7R3Dialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "DB2 UDB for AS/400".equals( databaseName ) ) {
				final int majorVersion = info.getDatabaseMajorVersion();
				final int minorVersion = info.getDatabaseMinorVersion();

				if ( majorVersion > 7 || ( majorVersion == 7 && minorVersion >= 3 ) ) {
					return latestDialectInstance( this );
				}
				else {
					return new DB2400Dialect();
				}
			}

			if ( databaseName.startsWith( "DB2/" ) ) {
				return new DB2Dialect();
			}

			return null;
		}
	},
	DERBY {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return DerbyTenSevenDialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "Apache Derby".equals( databaseName ) ) {
				final int majorVersion = info.getDatabaseMajorVersion();
				final int minorVersion = info.getDatabaseMinorVersion();

				if ( majorVersion > 10 || ( majorVersion == 10 && minorVersion >= 7 ) ) {
					return latestDialectInstance( this );
				}
				else if ( majorVersion == 10 && minorVersion == 6 ) {
					return new DerbyTenSixDialect();
				}
				else if ( majorVersion == 10 && minorVersion == 5 ) {
					return new DerbyTenFiveDialect();
				}
				else {
					return new DerbyDialect();
				}
			}

			return null;
		}
	},
	ENTERPRISEDB {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return PostgresPlusDialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "EnterpriseDB".equals( databaseName ) ) {
				return latestDialectInstance( this );
			}

			return null;
		}
	},
	FIREBIRD {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return FirebirdDialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( databaseName.startsWith( "Firebird" ) ) {
				return latestDialectInstance( this );
			}

			return null;
		}
	},
	FRONTBASE {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return FrontBaseDialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			return null;
		}
	},
	H2 {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return H2Dialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "H2".equals( databaseName ) ) {
				return latestDialectInstance( this );
			}

			return null;
		}
	},
	HANA {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return HANAColumnStoreDialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();
			int databaseMajorVersion = info.getDatabaseMajorVersion();

			if ( "HDB".equals( databaseName ) ) {
				// SAP recommends defaulting to column store.
				if ( databaseMajorVersion >= 4 ) {
					return new HANACloudColumnStoreDialect();
				}
				return latestDialectInstance( this );
			}

			return null;
		}
	},
	HSQL {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return HSQLDialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "HSQL Database Engine".equals( databaseName ) ) {
				return latestDialectInstance( this );
			}

			return null;
		}
	},
	INFORMIX {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return Informix10Dialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "Informix Dynamic Server".equals( databaseName ) ) {
				return latestDialectInstance( this );
			}

			return null;
		}
	},
	INGRES {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return Ingres10Dialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "ingres".equalsIgnoreCase( databaseName ) ) {
				final int majorVersion = info.getDatabaseMajorVersion();
				final int minorVersion = info.getDatabaseMinorVersion();

				if ( majorVersion < 9 ) {
					return new IngresDialect();
				}
				else if ( majorVersion == 9 ) {
					if ( minorVersion > 2 ) {
						return new Ingres9Dialect();
					}
					return new IngresDialect();
				}
				else if ( majorVersion == 10 ) {
					return new Ingres10Dialect();
				}

				return latestDialectInstance( this );
			}

			return null;
		}
	},
	INTERBASE {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return InterbaseDialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			return null;
		}
	},
	MARIADB {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return MariaDB103Dialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {

			if ( info.getDriverName() != null && info.getDriverName().startsWith( "MariaDB" ) ) {
				final int majorVersion = info.getDatabaseMajorVersion();
				final int minorVersion = info.getDatabaseMinorVersion();

				if ( majorVersion == 10 ) {
					if ( minorVersion >= 3 ) {
						return new MariaDB103Dialect();
					}
					else if ( minorVersion == 2 ) {
						return new MariaDB102Dialect();
					}
					else if ( minorVersion >= 0 ) {
						return new MariaDB10Dialect();
					}
					return new MariaDB53Dialect();
				}
				else if ( majorVersion > 5 || ( majorVersion == 5 && minorVersion >= 3 ) ) {
					return new MariaDB53Dialect();
				}
				return new MariaDBDialect();
			}

			return null;
		}
	},
	MAXDB {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return SAPDBDialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			return null;
		}
	},
	MCKOI {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return MckoiDialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			return null;
		}
	},
	MIMERSQL {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return MimerSQLDialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			return null;
		}
	},
	MYSQL {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return MySQL8Dialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "MySQL".equals( databaseName ) ) {
				final int majorVersion = info.getDatabaseMajorVersion();
				final int minorVersion = info.getDatabaseMinorVersion();

				if ( majorVersion < 5 ) {
					return new MySQLDialect();
				}
				else if ( majorVersion == 5 ) {
					if ( minorVersion < 5 ) {
						return new MySQL5Dialect();
					}
					else if ( minorVersion < 7 ) {
						return new MySQL55Dialect();
					}
					else {
						return new MySQL57Dialect();
					}
				}
				else if ( majorVersion < 8 ) {
					// There is no MySQL 6 or 7.
					// Adding this just in case.
					return new MySQL57Dialect();
				}
				else if ( majorVersion == 8 ) {
					return new MySQL8Dialect();
				}

				return latestDialectInstance( this );
			}

			return null;
		}
	},
	ORACLE {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return Oracle12cDialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "Oracle".equals( databaseName ) ) {
				final int majorVersion = info.getDatabaseMajorVersion();

				switch ( majorVersion ) {
					case 12:
						return new Oracle12cDialect();
					case 11:
						// fall through
					case 10:
						return new Oracle10gDialect();
					case 9:
						return new Oracle9iDialect();
					case 8:
						return new Oracle8iDialect();
					default:
						return latestDialectInstance( this );

				}
			}

			return null;
		}
	},
	POINTBASE {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return PointbaseDialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			return null;
		}
	},
	POSTGRESQL {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return PostgreSQL10Dialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "PostgreSQL".equals( databaseName ) ) {
				final int majorVersion = info.getDatabaseMajorVersion();
				final int minorVersion = info.getDatabaseMinorVersion();

				if ( majorVersion < 8 ) {
					return new PostgreSQL81Dialect();
				}

				if ( majorVersion == 8 ) {
					return minorVersion >= 2 ? new PostgreSQL82Dialect() : new PostgreSQL81Dialect();
				}

				if ( majorVersion == 9 ) {
					if ( minorVersion < 2 ) {
						return new PostgreSQL9Dialect();
					}
					else if ( minorVersion < 4 ) {
						return new PostgreSQL92Dialect();
					}
					else if ( minorVersion < 5 ) {
						return new PostgreSQL94Dialect();
					}
					else {
						return new PostgreSQL95Dialect();
					}
				}

				return latestDialectInstance( this );
			}

			return null;
		}
	},
	PROGRESS {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return ProgressDialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			return null;
		}
	},
	SQLSERVER {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return SQLServer2012Dialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( databaseName.startsWith( "Microsoft SQL Server" ) ) {
				final int majorVersion = info.getDatabaseMajorVersion();

				switch ( majorVersion ) {
					case 8: {
						return new SQLServerDialect();
					}
					case 9: {
						return new SQLServer2005Dialect();
					}
					case 10: {
						return new SQLServer2008Dialect();
					}
					case 11:
					case 12:
					case 13: {
						return new SQLServer2012Dialect();
					}
					default: {
						if ( majorVersion < 8 ) {
							return new SQLServerDialect();
						}
						else {
							// assume `majorVersion > 13`
							return latestDialectInstance( this );
						}
					}
				}
			}

			return null;
		}
	},
	SYBASE {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return SybaseASE15Dialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			final String databaseName = info.getDatabaseName();

			if ( "Sybase SQL Server".equals( databaseName ) || "Adaptive Server Enterprise".equals( databaseName ) ) {
				return latestDialectInstance( this );
			}

			if ( databaseName.startsWith( "Adaptive Server Anywhere" ) || "SQL Anywhere".equals( databaseName ) ) {
				return new SybaseAnywhereDialect();
			}

			return null;
		}
	},
	TERADATA {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return Teradata14Dialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			return null;
		}
	},
	TIMESTEN {
		@Override
		public Class<? extends Dialect> latestDialect() {
			return TimesTenDialect.class;
		}

		@Override
		public Dialect resolveDialect(DialectResolutionInfo info) {
			return null;
		}
	};

	public abstract Class<? extends Dialect> latestDialect();

	public abstract Dialect resolveDialect(DialectResolutionInfo info);

	private static Dialect latestDialectInstance(Database database) {
		try {
			return database.latestDialect().newInstance();
		}
		catch (InstantiationException | IllegalAccessException e) {
			throw new HibernateException( e );
		}
	}
}
