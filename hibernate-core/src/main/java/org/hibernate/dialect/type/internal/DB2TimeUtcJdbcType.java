package org.hibernate.dialect.type.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;

import org.hibernate.Internal;
import org.hibernate.type.descriptor.jdbc.TimeUtcAsLocalTimeJdbcType;

/// Descriptor which exchanges the time of day of an `OffsetTime` at UTC with
/// the DB2 JDBC driver as a [LocalTime].
///
/// As [TimeUtcAsLocalTimeJdbcType], apart from reading a null: the driver
/// fails with a [NullPointerException] when a null is requested as a
/// `LocalTime`.
///
/// @author Steve Ebersole
@Internal
public class DB2TimeUtcJdbcType extends TimeUtcAsLocalTimeJdbcType {
	public static final DB2TimeUtcJdbcType INSTANCE = new DB2TimeUtcJdbcType();

	@Override
	protected LocalTime getLocalTime(ResultSet rs, int index) throws SQLException {
		try {
			return super.getLocalTime( rs, index );
		}
		catch (NullPointerException e) {
			if ( rs.getObject( index ) == null ) {
				return null;
			}
			throw e;
		}
	}
}
