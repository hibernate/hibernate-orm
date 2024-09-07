/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.common;

import org.hibernate.dialect.Dialect;
import org.hibernate.query.SemanticException;

/**
 * A temporal field type which can occur as an argument
 * to {@code extract()} or as the unit of a duration
 * expression. A temporal field type may also occur as
 * an argument to {@code timestampadd()} or
 * {@code timestampdiff()}, in which case it is
 * interpreted as a unit of duration.
 * <p>
 * Note that not every {@code TemporalUnit} is legal
 * duration unit. The units of a duration are:
 * {@link #YEAR}, {@link #MONTH}, {@link #DAY},
 * {@link #HOUR}, {@link #MINUTE}, {@link #SECOND},
 * {@link #WEEK}, {@link #QUARTER}, and
 * {@link #NANOSECOND}.
 * <p>
 * Further note that accepted unit types in
 * {@code extract()} vary according to the type of the
 * second argument (date, time, or timestamp), and
 * according to capabilities of the database platform.
 *
 * @see Dialect#extractPattern(TemporalUnit)
 * @see org.hibernate.query.criteria.HibernateCriteriaBuilder#duration(long, TemporalUnit)
 * @see org.hibernate.query.criteria.HibernateCriteriaBuilder#durationByUnit(TemporalUnit, jakarta.persistence.criteria.Expression)
 *
 * @author Gavin King
 */
public enum TemporalUnit {
	/**
	 * Calendar year.
	 **/
	YEAR,
	/**
	 * Quarter, defined to mean three months.
	 **/
	QUARTER,
	/**
	 * Calendar month.
	 **/
	MONTH,
	/**
	 * Week, defined to mean 7 days when it occurs as a
	 * unit of duration, or to mean the ISO ISO-8601
	 * week number when passed to {@code extract()}. This
	 * is different to {@link #WEEK_OF_YEAR}.
	 **/
	WEEK,
	/**
	 * Day, defined to mean 24 hours when it occurs as a
	 * unit of duration, or to mean the calendar day of
	 * the month when passed to {@code extract()}.
	 **/
	DAY,
	/**
	 * Hour, defined to mean 60 minutes when it occurs as
	 * a unit of duration, or to mean the hour field in
	 * the range 0-23 (regular 24-hour time) when passed
	 * to {@code extract()}.
	 */
	HOUR,
	/**
	 * Minute, defined to mean 60 seconds when it occurs
	 * as a unit of duration, or to mean the minute field
	 * in the range 0-59 when passed to {@code extract()}.
	 */
	MINUTE,
	/**
	 * Second, defined to mean 1000 nanoseconds when it
	 * occurs as a unit of duration, or to mean the second
	 * field in the range 0-59 when passed to
	 * {@code extract()}. The second field includes
	 * fractional seconds (it is a floating point value).
	 */
	SECOND,
	/**
	 * Nanosecond, the basic most granular unit of duration.
	 * Few databases support billions-of-seconds, but Java's
	 * {@code Duration} type does. When it occurs as an
	 * argument to {@code extract()}, the nanosecond field
	 * is interpreted to include full seconds.
	 * <p>
	 * Note that the actual minimum granularity of a datetime
	 * varies by database platform (usually milliseconds or
	 * microseconds) so support for nanoseconds is emulated.
	 *
	 * @see #NATIVE
	 */
	NANOSECOND,
	/**
	 * The day of the week, from 1 (Sunday) to 7 (Saturday).
	 * <p>
	 * Not supported by every database platform.
	 */
	DAY_OF_WEEK,
	/**
	 * The day of the year, counting from 1.
	 * <p>
	 * Not supported by every database platform.
	 */
	DAY_OF_YEAR,
	/**
	 * The calendar day of the month, a synonym for {@link #DAY}.
	 */
	DAY_OF_MONTH,
	/**
	 * The week of the month, where the first day of the month
	 * is in week 1, and a new week starts each Sunday.
	 * <p>
	 * Supported on all platforms which natively support
	 * {@link #DAY_OF_WEEK}.
	 */
	WEEK_OF_MONTH,
	/**
	 * The week of the year, where the first day of the year
	 * is in week 1, and a new week starts each Sunday. This
	 * is different to {@link #WEEK}.
	 * <p>
	 * Supported on all platforms which natively support
	 * {@link #DAY_OF_WEEK} and {@link #DAY_OF_YEAR}.
	 */
	WEEK_OF_YEAR,
	/**
	 * The timezone offset of an offset datetime, as a
	 * {@link java.time.ZoneOffset}.
	 */
	OFFSET,
	/**
	 * The hour field of the {@link #OFFSET} in an offset
	 * datetime.
	 */
	TIMEZONE_HOUR,
	/**
	 * The minute field of the {@link #OFFSET} in an offset
	 * datetime.
	 */
	TIMEZONE_MINUTE,
	/**
	 * The date part of a timestamp, datetime, or offset datetime,
	 * as a {@link java.time.LocalDate}.
	 */
	DATE,
	/**
	 * The time part of a timestamp, datetime, or offset datetime,
	 * as a {@link java.time.LocalTime}.
	 */
	TIME,
	/**
	 * An internal value representing the Unix epoch, the elapsed
	 * seconds since January 1, 1970.
	 */
	EPOCH,
	/**
	 * An internal value representing the "native" resolution for
	 * date/time arithmetic of the underlying platform. Usually
	 * the smallest unit of fractional seconds, either milliseconds
	 * or microseconds. We define this value in order to avoid
	 * repeatedly converting between {@link #NANOSECOND}s and a
	 * unit that the database understands. On some platforms this
	 * is also used to avoid numeric overflow.
	 *
	 * @see Dialect#getFractionalSecondPrecisionInNanos()
	 */
	NATIVE;

	public String conversionFactor(TemporalUnit unit, Dialect dialect) {

		if ( unit == this ) {
			//same unit, nothing to do
			return "";
		}

		if ( unit.normalized() != normalized() ) {
			throw new SemanticException("Illegal unit conversion " + this + " to " + unit);
		}

		long from = normalizationFactor( dialect );
		long to = unit.normalizationFactor( dialect );
		if ( from == to ) {
			// the units represent the same amount of time
			return "";
		}
		else {
			// if from < to, then this unit represents a
			// smaller amount of time than the given unit
			// we are converting to (so we're going to
			// need to use division)
			return (from < to ? "/" : "*")
					+ factorAsString(from < to ? to / from : from / to);
		}
	}

	public String conversionFactorFull(TemporalUnit unit, Dialect dialect) {

		if ( unit == this ) {
			//same unit, nothing to do
			return "";
		}

		if ( unit.normalized() != normalized() ) {
			throw new SemanticException("Illegal unit conversion " + this + " to " + unit);
		}

		long from = normalizationFactor( dialect );
		long to = unit.normalizationFactor( dialect );
		if ( from == to ) {
			// the units represent the same amount of time
			return "";
		}
		else {
			// if from < to, then this unit represents a
			// smaller amount of time than the given unit
			// we are converting to (so we're going to
			// need to use division)
			return (from < to ? "/" : "*")
					+ (from < to ? to / from : from / to);
		}
	}

	/**
	 * The conversion factor required to convert this
	 * unit to its {@link #normalized()} unit.
	 */
	private long normalizationFactor(Dialect dialect) {
		long factor = 1;
		switch (this) {
			//conversion to days:
			case YEAR:
				factor *= 4;
			case QUARTER:
				factor *= 3;
			case MONTH:
				break;
			case WEEK:
				factor *= 7;
				//conversion to nanos:
			case DAY:
				factor *= 24;
			case HOUR:
				factor *= 60;
			case MINUTE:
				factor *= 60;
			case EPOCH:
			case SECOND:
				factor *= 1_000_000_000;
			case NANOSECOND:
				break;
			case NATIVE:
				factor *= dialect.getFractionalSecondPrecisionInNanos();
				break;
			default:
				throw new SemanticException("Inconvertible unit " + this);
		}
		return factor;
	}

	/**
	 * Obtain a fragment of SQL that can be used to perform
	 * a unit conversion. If the normalization factor is
	 * very large, represent it using exponential form so
	 * as to minimize the noise the generated SQL.
	 *
	 * @param factor the conversion factor
	 * @return a string to inject into the SQL expression
	 */
	private static String factorAsString(long factor) {
		String string = String.valueOf(factor);
		int len = string.length();
		int chop;
		for (chop = len; chop>0 && string.charAt(chop-1)=='0'; chop--) {}
		int e = len-chop;
		if (chop>0 && e>2) {
			string = string.substring(0, chop) + "e" + e;
		}
		return string;
	}

	/**
	 * Is this unit extractable from a date?
	 */
	public boolean isDateUnit() {
		switch (this) {
			case DAY:
			case WEEK:
			case MONTH:
			case QUARTER:
			case YEAR:
			case DAY_OF_WEEK:
			case DAY_OF_MONTH:
			case DAY_OF_YEAR:
			case WEEK_OF_MONTH:
			case WEEK_OF_YEAR:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Is this unit extractable from a time?
	 */
	public boolean isTimeUnit() {
		switch (this) {
			case HOUR:
			case MINUTE:
			case SECOND:
			case NATIVE:
			case NANOSECOND:
//			case EPOCH: //TODO!
			case TIMEZONE_HOUR:
			case TIMEZONE_MINUTE:
			case OFFSET:
				return true;
			default:
				return false;
		}
	}

	/**
	 * The unit that this unit "normalizes to",
	 * either {@link #NANOSECOND} or {@link #MONTH},
	 * which represent the two basic types of
	 * duration: "physical" durations, and "calendar"
	 * durations.
	 */
	public TemporalUnit normalized() {
		switch (this) {
			case NANOSECOND:
			case NATIVE:
			case HOUR:
			case MINUTE:
			case SECOND:
			case EPOCH:
			case DAY:
			case WEEK:
				return NANOSECOND;
			case YEAR:
			case QUARTER:
			case MONTH:
				return MONTH;
			default:
				throw new SemanticException("Illegal unit " + this);
		}
	}

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
