/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.dialect.internal;

import org.hibernate.dialect.CUBRIDDialect;
import org.hibernate.dialect.DB2400Dialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.DerbyTenFiveDialect;
import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.dialect.DerbyTenSixDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.FirebirdDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANAColumnStoreDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.InformixDialect;
import org.hibernate.dialect.Ingres10Dialect;
import org.hibernate.dialect.Ingres9Dialect;
import org.hibernate.dialect.IngresDialect;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.dialect.PostgreSQL92Dialect;
import org.hibernate.dialect.PostgreSQL94Dialect;
import org.hibernate.dialect.PostgreSQL9Dialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SQLServer2005Dialect;
import org.hibernate.dialect.SQLServer2008Dialect;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.SybaseAnywhereDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * The standard DialectResolver implementation
 *
 * @author Steve Ebersole
 */
public class StandardDialectResolver implements DialectResolver {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( StandardDialectResolver.class );

	/**
	 * Singleton access
	 */
	public static final StandardDialectResolver INSTANCE = new StandardDialectResolver();

	@Override
	public Dialect resolveDialect(DialectResolutionInfo info) {
		final String databaseName = info.getDatabaseName();

		if ( "CUBRID".equalsIgnoreCase( databaseName ) ) {
			return new CUBRIDDialect();
		}

		if ( "HSQL Database Engine".equals( databaseName ) ) {
			return new HSQLDialect();
		}

		if ( "H2".equals( databaseName ) ) {
			return new H2Dialect();
		}

		if ( "MySQL".equals( databaseName ) ) {
			final int majorVersion = info.getDatabaseMajorVersion();
			
			if (majorVersion >= 5 ) {
				return new MySQL5Dialect();
			}
			
			return new MySQLDialect();
		}

		if ( "PostgreSQL".equals( databaseName ) ) {
			final int majorVersion = info.getDatabaseMajorVersion();
			final int minorVersion = info.getDatabaseMinorVersion();

			if ( majorVersion == 9 ) {
				if ( minorVersion >= 4 ) {
					return new PostgreSQL94Dialect();
				}
				else if ( minorVersion >= 2 ) {
					return new PostgreSQL92Dialect();
				}
				return new PostgreSQL9Dialect();
			}

			if ( majorVersion == 8 && minorVersion >= 2 ) {
				return new PostgreSQL82Dialect();
			}
			
			return new PostgreSQL81Dialect();
		}
		
		if ( "EnterpriseDB".equals( databaseName ) ) {
			return new PostgresPlusDialect();
		}

		if ( "Apache Derby".equals( databaseName ) ) {
			final int majorVersion = info.getDatabaseMajorVersion();
			final int minorVersion = info.getDatabaseMinorVersion();

			if ( majorVersion > 10 || ( majorVersion == 10 && minorVersion >= 7 ) ) {
				return new DerbyTenSevenDialect();
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

		if ( "ingres".equalsIgnoreCase( databaseName ) ) {
			final int majorVersion = info.getDatabaseMajorVersion();
			final int minorVersion = info.getDatabaseMinorVersion();

			switch ( majorVersion ) {
				case 9:
					if (minorVersion > 2) {
						return new Ingres9Dialect();
					}
					return new IngresDialect();
				case 10:
					return new Ingres10Dialect();
				default:
					LOG.unknownIngresVersion( majorVersion );
			}
			return new IngresDialect();
		}

		if ( databaseName.startsWith( "Microsoft SQL Server" ) ) {
			final int majorVersion = info.getDatabaseMajorVersion();

			switch ( majorVersion ) {
				case 8:
					return new SQLServerDialect();
				case 9:
					return new SQLServer2005Dialect();
				case 10:
					return new SQLServer2008Dialect();
				case 11:
					return new SQLServer2012Dialect();
				default:
					LOG.unknownSqlServerVersion( majorVersion );
			}
			return new SQLServerDialect();
		}

		if ( "Sybase SQL Server".equals( databaseName ) || "Adaptive Server Enterprise".equals( databaseName ) ) {
			return new SybaseASE15Dialect();
		}

		if ( databaseName.startsWith( "Adaptive Server Anywhere" ) ) {
			return new SybaseAnywhereDialect();
		}

		if ( "Informix Dynamic Server".equals( databaseName ) ) {
			return new InformixDialect();
		}

		if ( "DB2 UDB for AS/400".equals( databaseName ) ) {
			return new DB2400Dialect();
		}

		if ( databaseName.startsWith( "DB2/" ) ) {
			return new DB2Dialect();
		}

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
					LOG.unknownOracleVersion( majorVersion );
			}
			return new Oracle8iDialect();
		}

		if ( "HDB".equals( databaseName ) ) {
			// SAP recommends defaulting to column store.
			return new HANAColumnStoreDialect();
		}

		if ( databaseName.startsWith( "Firebird" ) ) {
			return new FirebirdDialect();
		}

		return null;
	}
}
