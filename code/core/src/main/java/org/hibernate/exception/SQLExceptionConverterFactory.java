// $Id: SQLExceptionConverterFactory.java 4782 2004-11-21 00:11:27Z pgmjsd $
package org.hibernate.exception;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.StringHelper;

import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A factory for building SQLExceptionConverter instances.
 *
 * @author Steve Ebersole
 */
public class SQLExceptionConverterFactory {

	private static final Log log = LogFactory.getLog( SQLExceptionConverterFactory.class );

	private SQLExceptionConverterFactory() {
		// Private constructor - stops checkstyle from complaining.
	}

	/**
	 * Build a SQLExceptionConverter instance.
	 * <p/>
	 * First, looks for a {@link Environment.SQL_EXCEPTION_CONVERTER} property to see
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
			log.trace( "Using dialect defined converter" );
			converter = dialect.buildSQLExceptionConverter();
		}

		if ( converter instanceof Configurable ) {
			try {
				( ( Configurable ) converter ).configure( properties );
			}
			catch ( HibernateException e ) {
				log.warn( "Unable to configure SQLExceptionConverter", e );
				throw e;
			}
		}

		return converter;
	}

	/**
	 * Builds a minimal converter.  The instance returned here just always converts to
	 * {@link GenericJDBCException}.
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
			log.trace( "Attempting to construct instance of specified SQLExceptionConverter [" + converterClassName + "]" );
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
			log.warn( "Unable to construct instance of specified SQLExceptionConverter", t );
		}

		return null;
	}
}
