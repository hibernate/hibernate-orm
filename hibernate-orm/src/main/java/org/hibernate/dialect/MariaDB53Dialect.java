/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.StaticPrecisionFspTimestampFunction;

/**
 * @author Vlad Mihalcea
 */
public class MariaDB53Dialect extends MariaDBDialect {
	public MariaDB53Dialect() {
		super();

		// For details about MariaDB 5.3 support for fractional seconds
		// precision (fsp): https://mariadb.com/kb/en/mariadb/microseconds-in-mariadb/
		// Regarding datetime(fsp), "The fsp value, if given, must be
		// in the range 0 to 6. A value of 0 signifies that there is
		// no fractional part. If omitted, the default precision is 0.
		// (This differs from the standard SQL default of 6, for
		// compatibility with previous MariaDB versions.)".

		// The following is defined because Hibernate currently expects
		// the SQL 1992 default of 6 (which is inconsistent with the MariaDB
		// default).
		registerColumnType( Types.TIMESTAMP, "datetime(6)" );

		// MariaDB also supports fractional seconds precision for time values
		// (time(fsp)). According to SQL 1992, the default for <time precision>
		// is 0. The MariaDB default is time(0), there's no need to override
		// the setting for Types.TIME columns.

		// For details about MariaDB support for timestamp functions, see:
		// http://dev.MariaDB.com/doc/refman/5.7/en/date-and-time-functions.html

		// The following are synonyms for now(fsp), where fsp defaults to 0 on MariaDB 5.3:
		// current_timestamp([fsp]), localtime(fsp), localtimestamp(fsp).
		// Register the same StaticPrecisionFspTimestampFunction for all 4 functions.
		final SQLFunction currentTimestampFunction = new StaticPrecisionFspTimestampFunction( "now", 6 );

		registerFunction( "now", currentTimestampFunction );
		registerFunction( "current_timestamp", currentTimestampFunction );
		registerFunction( "localtime", currentTimestampFunction );
		registerFunction( "localtimestamp", currentTimestampFunction );

		// sysdate is different from now():
		// "SYSDATE() returns the time at which it executes. This differs
		// from the behavior for NOW(), which returns a constant time that
		// indicates the time at which the statement began to execute.
		// (Within a stored function or trigger, NOW() returns the time at
		// which the function or triggering statement began to execute.)
		registerFunction( "sysdate", new StaticPrecisionFspTimestampFunction( "sysdate", 6 ) );

		// from_unixtime(), timestamp() are functions that return TIMESTAMP that do not support a
		// fractional seconds precision argument (so there's no need to override them here):
	}
}
