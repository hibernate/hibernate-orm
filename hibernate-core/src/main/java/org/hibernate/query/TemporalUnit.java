/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import org.hibernate.query.sqm.SemanticException;

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
 * @see org.hibernate.dialect.Dialect#extract(TemporalUnit)
 *
 * @author Gavin King
 */
public enum TemporalUnit {
	/**
	 * Calendar year.
	 **/
	YEAR(true),
	/**
	 * Quarter, defined to mean three months.
	 **/
	QUARTER(true),
	/**
	 * Calendar month.
	 **/
	MONTH(true),
	/**
	 * Week, defined to mean 7 days when it occurs as a
	 * unit of duration, or to mean the ISO ISO-8601
	 * week number when passed to {@code extract()}. This
	 * is different to {@link #WEEK_OF_YEAR}.
	 **/
	WEEK(true),
	/**
	 * Day, defined to mean 24 hours when it occurs as a
	 * unit of duration, or to mean the calendar day of
	 * the month when passed to {@code extract()}.
	 **/
	DAY(true),
	/**
	 * Hour, defined to mean 60 minutes when it occurs as
	 * a unit of duration, or to mean the hour field in
	 * the range 0-23 (regular 24-hour time) when passed
	 * to {@code extract()}.
	 */
	HOUR(false),
	/**
	 * Minute, defined to mean 60 seconds when it occurs
	 * as a unit of duration, or to mean the minute field
	 * in the range 0-59 when passed to {@code extract()}.
	 */
	MINUTE(false),
	/**
	 * Second, defined to mean 1000 nanoseconds when it
	 * occurs as a unit of duration, or to mean the second
	 * field in the range 0-59 when passed to
	 * {@code extract()}. The second field includes
	 * fractional seconds (it is a floating point value).
	 */
	SECOND(false),
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
	 */
	NANOSECOND(false),
	/**
	 * The day of the week, from 1 (Sunday) to 7 (Saturday).
	 * <p>
	 * Not supported by every database platform.
	 */
	DAY_OF_WEEK(true),
	/**
	 * The day of the year, counting from 1.
	 * <p>
	 * Not supported by every database platform.
	 */
	DAY_OF_YEAR(true),
	/**
	 * The calendar day of the month, a synonym for {@link #DAY}.
	 */
	DAY_OF_MONTH(true),
	/**
	 * The week of the month, where the first day of the month
	 * is in week 1, and a new week starts each Sunday.
	 * <p>
	 * Supported on all platforms which natively support
	 * {@link #DAY_OF_WEEK}.
	 */
	WEEK_OF_MONTH(true),
	/**
	 * The week of the year, where the first day of the year
	 * is in week 1, and a new week starts each Sunday. This
	 * is different to {@link #WEEK}.
	 * <p>
	 * Supported on all platforms which natively support
	 * {@link #DAY_OF_WEEK} and {@link #DAY_OF_YEAR}.
	 */
	WEEK_OF_YEAR(true),
	/**
	 * The timezone offset of an offset datetime, as a
	 * {@link java.time.ZoneOffset}.
	 */
	OFFSET(false),
	/**
	 * The hour field of the {@link #OFFSET} in an offset
	 * datetime.
	 */
	TIMEZONE_HOUR(false),
	/**
	 * The minute field of the {@link #OFFSET} in an offset
	 * datetime.
	 */
	TIMEZONE_MINUTE(false),
	/**
	 * The date part of a timestamp, datetime, or offset datetime,
	 * as a {@link java.time.LocalDate}.
	 */
	DATE(false),
	/**
	 * The time part of a timestamp, datetime, or offset datetime,
	 * as a {@link java.time.LocalTime}.
	 */
	TIME(false),
	/**
	 * An internal value representing the Unix epoch, the elapsed
	 * seconds since January 1, 1970. Currently not supported in
	 * HQL.
	 */
	EPOCH(false);

	private boolean dateUnit;

	TemporalUnit(boolean dateUnit) {
		this.dateUnit = dateUnit;
	}

	private static void illegalConversion(TemporalUnit from, TemporalUnit to) {
		throw new SemanticException("illegal unit conversion " + from + " to " + to);
	}

	public String conversionFactor(TemporalUnit toUnit) {
		return conversionFactor(this, toUnit);
	}

	public static String conversionFactor(TemporalUnit fromUnit, TemporalUnit toUnit) {
		if (fromUnit == EPOCH) {
			fromUnit = SECOND;
		}
		if (toUnit == EPOCH) {
			toUnit = SECOND;
		}
		long factor = 1;
		boolean reciprocal = false;
		if (toUnit == fromUnit) {
			return "";
		}
		else {
			switch (toUnit) {
				case NANOSECOND:
					switch (fromUnit) {
						case WEEK:
							factor *= 7;
						case DAY:
							factor *= 24;
						case HOUR:
							factor *= 60;
						case MINUTE:
							factor *= 60;
						case SECOND:
							factor *= 1e9;
							break;
						default:
							illegalConversion(fromUnit, toUnit);
					}
					break;
				case SECOND:
					switch (fromUnit) {
						case NANOSECOND:
							factor *= 1e9;
							reciprocal = true;
							break;
						case WEEK:
							factor *= 7;
						case DAY:
							factor *= 24;
						case HOUR:
							factor *= 60;
						case MINUTE:
							factor *= 60;
							break;
						default:
							illegalConversion(fromUnit, toUnit);
					}
					break;
				case MINUTE:
					switch (fromUnit) {
						case NANOSECOND:
							factor *= 1e9;
						case SECOND:
							factor *= 60;
							reciprocal = true;
							break;
						case WEEK:
							factor *= 7;
						case DAY:
							factor *= 24;
						case HOUR:
							factor *= 60;
							break;
						default:
							illegalConversion(fromUnit, toUnit);
					}
					break;
				case HOUR:
					switch (fromUnit) {
						case NANOSECOND:
							factor *= 1e9;
						case SECOND:
							factor *= 60;
						case MINUTE:
							factor *= 60;
							reciprocal = true;
							break;
						case WEEK:
							factor *= 7;
						case DAY:
							factor *= 24;
							break;
						default:
							illegalConversion(fromUnit, toUnit);
					}
					break;
				case DAY:
					switch (fromUnit) {
						case NANOSECOND:
							factor *= 1e9;
						case SECOND:
							factor *= 60;
						case MINUTE:
							factor *= 60;
						case HOUR:
							factor *= 24;
							reciprocal = true;
							break;
						case WEEK:
							factor *= 7;
							break;
						default:
							illegalConversion(fromUnit, toUnit);
					}
					break;
				case WEEK:
					switch (fromUnit) {
						case NANOSECOND:
							factor *= 1e9;
						case SECOND:
							factor *= 60;
						case MINUTE:
							factor *= 60;
						case HOUR:
							factor *= 24;
						case DAY:
							factor *= 7;
							reciprocal = true;
							break;
						default:
							illegalConversion(fromUnit, toUnit);
					}
					break;
				case MONTH:
					switch (fromUnit) {
						case YEAR:
							factor *= 4;
						case QUARTER:
							factor *= 3;
							break;
						default:
							illegalConversion(fromUnit, toUnit);
					}
					break;
				case QUARTER:
					switch (fromUnit) {
						case MONTH:
							factor *= 3;
							break;
						case YEAR:
							factor *= 4;
							reciprocal = true;
							break;
						default:
							illegalConversion(fromUnit, toUnit);
					}
					break;
				case YEAR:
					switch (fromUnit) {
						case MONTH:
							factor *= 3;
						case QUARTER:
							factor *= 4;
							reciprocal = true;
							break;
						default:
							illegalConversion(fromUnit, toUnit);
					}
					break;
				default:
					illegalConversion(fromUnit, toUnit);
			}
			String string = String.valueOf(factor);
			int len = string.length();
			int chop;
			for (chop = len; chop>0 && string.charAt(chop-1)=='0'; chop--) {}
			int e = len-chop;
			if (chop>0 && e>2) {
				string = string.substring(0, chop) + "e" + e;
			}
			return (reciprocal ? "/" : "*") + string;
		}
	}

	public boolean isDateUnit() {
		return dateUnit;
	}

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
