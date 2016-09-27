/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.exception.spi;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;

import org.jboss.logging.Logger;

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

		String converterClassName = (String) properties.get( Environment.SQL_EXCEPTION_CONVERTER );
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
			catch (HibernateException e) {
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
			final Class converterClass = ReflectHelper.classForName( converterClassName );

			// First, try to find a matching constructor accepting a ViolatedConstraintNameExtracter param...
			final Constructor[] ctors = converterClass.getDeclaredConstructors();
			for ( Constructor ctor : ctors ) {
				if ( ctor.getParameterTypes() != null && ctor.getParameterCount() == 1 ) {
					if ( ViolatedConstraintNameExtracter.class.isAssignableFrom( ctor.getParameterTypes()[0] ) ) {
						try {
							return (SQLExceptionConverter) ctor.newInstance( violatedConstraintNameExtracter );
						}
						catch (Throwable ignore) {
							// eat it and try next
						}
					}
				}
			}

			// Otherwise, try to use the no-arg constructor
			return (SQLExceptionConverter) converterClass.newInstance();

		}
		catch (Throwable t) {
			LOG.unableToConstructSqlExceptionConverter( t );
		}

		return null;
	}
}
