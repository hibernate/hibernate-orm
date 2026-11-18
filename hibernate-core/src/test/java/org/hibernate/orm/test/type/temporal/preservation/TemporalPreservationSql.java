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

	/// The seconds from the epoch to the instant in the column `theValue`, for SQL Server.
	static final String SQL_SERVER_EPOCH =
			"datediff_big(second, cast('1970-01-01 00:00:00 +00:00' as datetimeoffset), theValue)";
}
