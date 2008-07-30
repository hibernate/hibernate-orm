/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.dialect;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.util.ReflectHelper;

/**
 * A factory for generating Dialect instances.
 *
 * @author Steve Ebersole
 */
public class DialectFactory {

	/**
	 * Builds an appropriate Dialect instance.
	 * <p/>
	 * If a dialect is explicitly named in the incoming properties, it is used. Otherwise, the database name and version
	 * (obtained from connection metadata) are used to make the dertemination.
	 * <p/>
	 * An exception is thrown if a dialect was not explicitly set and the database name is not known.
	 *
	 * @param props The configuration properties.
	 * @param databaseName The name of the database product (obtained from metadata).
	 * @param databaseMajorVersion The major version of the database product (obtained from metadata).
	 *
	 * @return The appropriate dialect.
	 *
	 * @throws HibernateException No dialect specified and database name not known.
	 */
	public static Dialect buildDialect(Properties props, String databaseName, int databaseMajorVersion)
	        throws HibernateException {
		String dialectName = props.getProperty( Environment.DIALECT );
		if ( dialectName == null ) {
			return determineDialect( databaseName, databaseMajorVersion );
		}
		else {
			return buildDialect( dialectName );
		}
	}

	/**
	 * Determine the appropriate Dialect to use given the database product name
	 * and major version.
	 *
	 * @param databaseName The name of the database product (obtained from metadata).
	 * @param databaseMajorVersion The major version of the database product (obtained from metadata).
	 *
	 * @return An appropriate dialect instance.
	 */
	public static Dialect determineDialect(String databaseName, int databaseMajorVersion) {
		if ( databaseName == null ) {
			throw new HibernateException( "Hibernate Dialect must be explicitly set" );
		}

		DatabaseDialectMapper mapper = ( DatabaseDialectMapper ) MAPPERS.get( databaseName );
		if ( mapper == null ) {
			throw new HibernateException( "Hibernate Dialect must be explicitly set for database: " + databaseName );
		}

		String dialectName = mapper.getDialectClass( databaseMajorVersion );
		return buildDialect( dialectName );
	}

	/**
	 * Returns a dialect instance given the name of the class to use.
	 *
	 * @param dialectName The name of the dialect class.
	 *
	 * @return The dialect instance.
	 */
	public static Dialect buildDialect(String dialectName) {
		try {
			return ( Dialect ) ReflectHelper.classForName( dialectName ).newInstance();
		}
		catch ( ClassNotFoundException cnfe ) {
			throw new HibernateException( "Dialect class not found: " + dialectName );
		}
		catch ( Exception e ) {
			throw new HibernateException( "Could not instantiate dialect class", e );
		}
	}

	/**
	 * For a given database product name, instances of
	 * DatabaseDialectMapper know which Dialect to use for different versions.
	 */
	public static interface DatabaseDialectMapper {
		public String getDialectClass(int majorVersion);
	}

	/**
	 * A simple DatabaseDialectMapper for dialects which are independent
	 * of the underlying database product version.
	 */
	public static class VersionInsensitiveMapper implements DatabaseDialectMapper {
		private String dialectClassName;

		public VersionInsensitiveMapper(String dialectClassName) {
			this.dialectClassName = dialectClassName;
		}

		public String getDialectClass(int majorVersion) {
			return dialectClassName;
		}
	}

	// TODO : this is the stuff it'd be nice to move to a properties file or some other easily user-editable place
	private static final Map MAPPERS = new HashMap();
	static {
		// detectors...
		MAPPERS.put( "HSQL Database Engine", new VersionInsensitiveMapper( "org.hibernate.dialect.HSQLDialect" ) );
		MAPPERS.put( "H2", new VersionInsensitiveMapper( "org.hibernate.dialect.H2Dialect" ) );
		MAPPERS.put( "MySQL", new VersionInsensitiveMapper( "org.hibernate.dialect.MySQLDialect" ) );
		MAPPERS.put( "PostgreSQL", new VersionInsensitiveMapper( "org.hibernate.dialect.PostgreSQLDialect" ) );
		MAPPERS.put( "Apache Derby", new VersionInsensitiveMapper( "org.hibernate.dialect.DerbyDialect" ) );

		MAPPERS.put( "Ingres", new VersionInsensitiveMapper( "org.hibernate.dialect.IngresDialect" ) );
		MAPPERS.put( "ingres", new VersionInsensitiveMapper( "org.hibernate.dialect.IngresDialect" ) );
		MAPPERS.put( "INGRES", new VersionInsensitiveMapper( "org.hibernate.dialect.IngresDialect" ) );

		MAPPERS.put( "Microsoft SQL Server Database", new VersionInsensitiveMapper( "org.hibernate.dialect.SQLServerDialect" ) );
		MAPPERS.put( "Microsoft SQL Server", new VersionInsensitiveMapper( "org.hibernate.dialect.SQLServerDialect" ) );
		MAPPERS.put( "Sybase SQL Server", new VersionInsensitiveMapper( "org.hibernate.dialect.SybaseDialect" ) );
		MAPPERS.put( "Adaptive Server Enterprise", new VersionInsensitiveMapper( "org.hibernate.dialect.SybaseDialect" ) );

		MAPPERS.put( "Informix Dynamic Server", new VersionInsensitiveMapper( "org.hibernate.dialect.InformixDialect" ) );

		// thanks goodness for "universal" databases...
		MAPPERS.put( "DB2/NT", new VersionInsensitiveMapper( "org.hibernate.dialect.DB2Dialect" ) );
		MAPPERS.put( "DB2/LINUX", new VersionInsensitiveMapper( "org.hibernate.dialect.DB2Dialect" ) );
		MAPPERS.put( "DB2/6000", new VersionInsensitiveMapper( "org.hibernate.dialect.DB2Dialect" ) );
		MAPPERS.put( "DB2/HPUX", new VersionInsensitiveMapper( "org.hibernate.dialect.DB2Dialect" ) );
		MAPPERS.put( "DB2/SUN", new VersionInsensitiveMapper( "org.hibernate.dialect.DB2Dialect" ) );
		MAPPERS.put( "DB2/LINUX390", new VersionInsensitiveMapper( "org.hibernate.dialect.DB2Dialect" ) );
		MAPPERS.put( "DB2/AIX64", new VersionInsensitiveMapper( "org.hibernate.dialect.DB2Dialect" ) );

		MAPPERS.put(
		        "Oracle",
		        new DatabaseDialectMapper() {
			        public String getDialectClass(int majorVersion) {
						switch ( majorVersion ) {
							case 8: return Oracle8iDialect.class.getName();
							case 9: return Oracle9iDialect.class.getName();
							case 10: return Oracle10gDialect.class.getName();
							default: throw new HibernateException( "unknown Oracle major version [" + majorVersion + "]" );
						}
			        }
		        }
		);
	}
}
