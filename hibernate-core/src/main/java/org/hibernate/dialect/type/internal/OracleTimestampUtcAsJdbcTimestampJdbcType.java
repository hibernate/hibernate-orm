package org.hibernate.dialect.type.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.TimeZone;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsZonedJdbcTimestampJdbcType;

/// Oracle's generic zoned Timestamp binding depends on session timezone rules
/// for historical values. Construct an explicitly UTC TIMESTAMPTZ datum instead,
/// retaining the matching Timestamp/UTC-Calendar encoding and extraction.
/// Driver reflection belongs to the descriptor's bootstrap classloader context;
/// constructing the descriptor does not obtain a connection or JDBC metadata.
///
/// @since 8.0
/// @author Steve Ebersole
public final class OracleTimestampUtcAsJdbcTimestampJdbcType extends TimestampUtcAsZonedJdbcTimestampJdbcType {
	private final Constructor<?> constructor;

	public OracleTimestampUtcAsJdbcTimestampJdbcType(ClassLoaderService classLoader) {
		try {
			constructor = classLoader.classForName( "oracle.sql.TIMESTAMPTZ" )
					.getConstructor( Connection.class, Timestamp.class, Calendar.class );
		}
		catch (ReflectiveOperationException ex) {
			throw new HibernateException( "Could not resolve Oracle UTC Timestamp JDBC constructor", ex );
		}
	}

	private Object utcDatum(PreparedStatement statement, Timestamp value) throws SQLException {
		try {
			return constructor.newInstance( statement.getConnection(), value, Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ) );
		}
		catch (InvocationTargetException ex) {
			if ( ex.getCause() instanceof SQLException sqlException ) throw sqlException;
			throw new SQLException( "Could not encode Oracle UTC Timestamp", ex.getCause() );
		}
		catch (ReflectiveOperationException ex) {
			throw new SQLException( "Could not construct Oracle UTC Timestamp", ex );
		}
	}

	@Override
	protected void bindTimestamp(PreparedStatement statement, Timestamp value, int index) throws SQLException {
		statement.setObject( index, utcDatum( statement, value ), Types.TIMESTAMP_WITH_TIMEZONE );
	}

	@Override
	protected void bindTimestamp(CallableStatement statement, Timestamp value, String name) throws SQLException {
		statement.setObject( name, utcDatum( statement, value ), Types.TIMESTAMP_WITH_TIMEZONE );
	}
}
