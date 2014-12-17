/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.StaticPrecisionFspTimestampFunction;

/**
 * @author Gail Badner
 */
public class MySQL57InnoDBDialect extends MySQL5InnoDBDialect {
	public MySQL57InnoDBDialect() {
		super();

		// For details about MySQL 5.7 support for fractional seconds
		// precision (fsp): http://dev.mysql.com/doc/refman/5.7/en/fractional-seconds.html
		// Regarding datetime(fsp), "The fsp value, if given, must be
		// in the range 0 to 6. A value of 0 signifies that there is
		// no fractional part. If omitted, the default precision is 0.
		// (This differs from the standard SQL default of 6, for
		// compatibility with previous MySQL versions.)".

		// The following is defined because Hibernate currently expects
		// the SQL 1992 default of 6 (which is inconsistent with the MySQL
		// default).
		registerColumnType( Types.TIMESTAMP, "datetime(6)" );

		// MySQL also supports fractional seconds precision for time values
		// (time(fsp)). According to SQL 1992, the default for <time precision>
		// is 0. The MySQL default is time(0), there's no need to override
		// the setting for Types.TIME columns.

		// For details about MySQL support for timestamp functions, see:
		// http://dev.mysql.com/doc/refman/5.7/en/date-and-time-functions.html

		// The following are synonyms for now(fsp), where fsp defaults to 0 on MySQL 5.7:
		// current_timestamp([fsp]), localtime(fsp), localtimestamp(fsp).
		// Register the same StaticPrecisionFspTimestampFunction for all 4 functions.
		final SQLFunction currentTimestampFunction = new StaticPrecisionFspTimestampFunction("now", 6 );

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
