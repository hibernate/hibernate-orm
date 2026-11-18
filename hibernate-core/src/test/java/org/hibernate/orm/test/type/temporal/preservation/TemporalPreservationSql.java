package org.hibernate.orm.test.type.temporal.preservation;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;

/// The literals of the database's own SQL which the tests use to write and
/// compare values without Hibernate being involved.
///
/// @author Steve Ebersole
final class TemporalPreservationSql {
	private TemporalPreservationSql() {
	}

	/// A date, given as `yyyy-MM-dd`.
	static String dateLiteral(Dialect dialect, String fields) {
		return dialect instanceof SQLServerDialect ? "cast('" + fields + "' as date)" : "date '" + fields + "'";
	}

	/// A date and time without a time zone, given as `yyyy-MM-dd HH:mm:ss` with any fraction.
	static String dateTimeLiteral(Dialect dialect, String fields) {
		return dialect instanceof SQLServerDialect
				? "cast('" + fields + "' as datetime2)"
				: "timestamp '" + fields + "'";
	}

	/// A time of day, given as `HH:mm:ss` with any fraction.
	static String timeLiteral(Dialect dialect, String fields) {
		return dialect instanceof SQLServerDialect ? "cast('" + fields + "' as time)" : "time '" + fields + "'";
	}

	/// An instant, given as its date and time at UTC, for SQL Server.
	static String sqlServerInstantLiteral(String fields) {
		return "cast('" + fields + " +00:00' as datetimeoffset)";
	}

	/// An instant, given as its date and time at UTC, for HSQLDB: for a
	/// column which holds an instant with an offset, or for one which holds
	/// the date and time at UTC and knows of no time zone.
	static String hsqlInstantLiteral(String fields, boolean withTimeZone) {
		return "timestamp '" + fields + ( withTimeZone ? "+00:00'" : "'" );
	}

	/// The seconds from the epoch to the instant in the column `theValue`, for HSQLDB.
	static String hsqlEpoch(boolean withTimeZone) {
		final String epoch = hsqlInstantLiteral( "1970-01-01 00:00:00", withTimeZone );
		return "datediff('second', " + epoch + ", theValue)"
			+ " - (case when theValue < " + epoch
			+ " and extract(second from theValue) <> floor(extract(second from theValue)) then 1 else 0 end)";
	}

	/// The statement which sets the time zone of a session of HSQLDB, which
	/// knows offsets and no regions, to the offset the zone has in winter.
	static String hsqlSessionZone(String zone) {
		final String offset = switch ( zone ) {
			case "Asia/Kathmandu" -> "+5:45";
			case "America/New_York" -> "-5:00";
			case "Asia/Manila" -> "+8:00";
			default -> throw new IllegalArgumentException( zone );
		};
		return "set time zone interval '" + offset + "' hour to minute";
	}

	/// The seconds from the epoch to the instant in the column `theValue`, for
	/// DB2, where the column holds the date and time at UTC.
	static final String DB2_EPOCH =
			"cast(days(theValue) - days(date '1970-01-01') as bigint) * 86400 + midnight_seconds(theValue)";

	/// The seconds from the epoch to the instant in the column `theValue`, for SQL Server.
	static final String SQL_SERVER_EPOCH =
			"datediff_big(second, cast('1970-01-01 00:00:00 +00:00' as datetimeoffset), theValue)";
}
