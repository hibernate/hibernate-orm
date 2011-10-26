/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.exception.spi;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.Properties;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;

/**
 * A factory for building SQLExceptionConverter instances.
 *
 * @author Steve Ebersole
 */
public class SQLExceptionConverterFactory {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, SQLExceptionConverterFactory.class.getName() );

	private SQLExceptionConverterFactory() {
		// Private constructor - stops checkstyle from complaining.
	}

	/**
	 * Build a SQLExceptionConverter instance.
	 * <p/>
	 * First, looks for a {@link Environment#SQL_EXCEPTION_CONVERTER} property to see
	 * if the configuration specified the class of a specific converter to use.  If this
	 * property is set, attempt to construct an instance of that class.  If not set, or
	 * if construction fails, the converter specific to the dialect will be used.
	 *
	 * @param dialect    The defined dialect.
	 * @param properties The configuration properties.
	 * @return An appropriate SQLExceptionConverter instance.
	 * @throws HibernateException There was an error building the SQLExceptionConverter.
	 */
	public static SQLExceptionConverter buildSQLExceptionConverter(Dialect dialect, Properties properties) throws HibernateException {
		SQLExceptionConverter converter = null;

		String converterClassName = ( String ) properties.get( Environment.SQL_EXCEPTION_CONVERTER );
		if ( StringHelper.isNotEmpty( converterClassName ) ) {
			converter = constructConverter( converterClassName, dialect.getViolatedConstraintNameExtracter() );
		}

		if ( converter == null ) {
			LOG.trace( "Using dialect defined converter" );
			converter = dialect.buildSQLExceptionConverter();
		}

		if ( converter instanceof Configurable ) {
			try {
				( (Configurable) converter ).configure( properties );
			}
			catch ( HibernateException e ) {
				LOG.unableToConfigureSqlExceptionConverter( e );
				throw e;
			}
		}

		return converter;
	}

	/**
	 * Builds a minimal converter.  The instance returned here just always converts to
	 * {@link org.hibernate.exception.GenericJDBCException}.
	 *
	 * @return The minimal converter.
	 */
	public static SQLExceptionConverter buildMinimalSQLExceptionConverter() {
		return new SQLExceptionConverter() {
			public JDBCException convert(SQLException sqlException, String message, String sql) {
				return new GenericJDBCException( message, sqlException, sql );
			}
		};
	}

	private static SQLExceptionConverter constructConverter(String converterClassName, ViolatedConstraintNameExtracter violatedConstraintNameExtracter) {
		try {
			LOG.tracev( "Attempting to construct instance of specified SQLExceptionConverter [{0}]", converterClassName );
			Class converterClass = ReflectHelper.classForName( converterClassName );

			// First, try to find a matching constructor accepting a ViolatedConstraintNameExtracter param...
			Constructor[] ctors = converterClass.getDeclaredConstructors();
			for ( int i = 0; i < ctors.length; i++ ) {
				if ( ctors[i].getParameterTypes() != null && ctors[i].getParameterTypes().length == 1 ) {
					if ( ViolatedConstraintNameExtracter.class.isAssignableFrom( ctors[i].getParameterTypes()[0] ) ) {
						try {
							return ( SQLExceptionConverter )
									ctors[i].newInstance( new Object[]{violatedConstraintNameExtracter} );
						}
						catch ( Throwable t ) {
							// eat it and try next
						}
					}
				}
			}

			// Otherwise, try to use the no-arg constructor
			return ( SQLExceptionConverter ) converterClass.newInstance();

		}
		catch ( Throwable t ) {
			LOG.unableToConstructSqlExceptionConverter( t );
		}

		return null;
	}
}
