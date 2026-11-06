package org.hibernate.dialect.type.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

import org.hibernate.type.descriptor.jdbc.TimestampUtcAsZonedJdbcTimestampJdbcType;

/// PostgreSQL accepts UTC-Calendar Timestamp binding for timestamptz, but rejects
/// setObject(Timestamp, TIMESTAMP_WITH_TIMEZONE). Keep that variation in its JdbcType.
///
/// @since 8.0
/// @author Steve Ebersole
public final class PostgreSQLTimestampUtcAsJdbcTimestampJdbcType extends TimestampUtcAsZonedJdbcTimestampJdbcType {
	public static final PostgreSQLTimestampUtcAsJdbcTimestampJdbcType INSTANCE = new PostgreSQLTimestampUtcAsJdbcTimestampJdbcType();

	@Override
	protected void bindTimestamp(PreparedStatement statement, Timestamp value, int index) throws SQLException {
		statement.setTimestamp( index, value, Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ) );
	}

	@Override
	protected void bindTimestamp(CallableStatement statement, Timestamp value, String name) throws SQLException {
		statement.setTimestamp( name, value, Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ) );
	}
}
