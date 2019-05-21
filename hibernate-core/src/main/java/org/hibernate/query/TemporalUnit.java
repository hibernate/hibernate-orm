/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import org.hibernate.query.sqm.SemanticException;

/**
 * @author Gavin King
 */
public enum TemporalUnit {
	YEAR(true), QUARTER(true), MONTH(true), WEEK(true), DAY(true),
	HOUR(false), MINUTE(false),
	SECOND(false), MILLISECOND(false), MICROSECOND(false),
	NANOSECOND(false),
	DAY_OF_WEEK(true), DAY_OF_YEAR(true), DAY_OF_MONTH(true),
	OFFSET(false), TIMEZONE_HOUR(false), TIMEZONE_MINUTE(false);

	private boolean dateUnit;

	TemporalUnit(boolean dateUnit) {
		this.dateUnit = dateUnit;
	}

	private static void illegalConversion(TemporalUnit from, TemporalUnit to) {
		throw new SemanticException("illegal unit conversion " + from + " to " + to);
	}

	public static String conversionFactor(TemporalUnit fromUnit, TemporalUnit toUnit) {
		long factor = 1;
		boolean reciprocal = false;
		if ( toUnit != fromUnit ) {
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
							factor *= 1e3;
						case MILLISECOND:
							factor *= 1e3;
						case MICROSECOND:
							factor *= 1e3;
							break;
						default:
							illegalConversion(fromUnit, toUnit);
					}
					break;
				case MICROSECOND:
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
							factor *= 1e3;
						case MILLISECOND:
							factor *= 1e3;
							break;
						case NANOSECOND:
							factor *= 1e3;
							reciprocal = true;
							break;
						default:
							illegalConversion(fromUnit, toUnit);
					}
					break;
				case MILLISECOND:
					switch (fromUnit) {
						case NANOSECOND:
							factor *= 1e3;
						case MICROSECOND:
							factor *= 1e3;
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
						case SECOND:
							factor *= 1e3;
							break;
						default:
							illegalConversion(fromUnit, toUnit);
					}
					break;
				case SECOND:
					switch (fromUnit) {
						case NANOSECOND:
							factor *= 1e3;
						case MICROSECOND:
							factor *= 1e3;
						case MILLISECOND:
							factor *= 1e3;
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
							factor *= 1e3;
						case MICROSECOND:
							factor *= 1e3;
						case MILLISECOND:
							factor *= 1e3;
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
							factor *= 1e3;
						case MICROSECOND:
							factor *= 1e3;
						case MILLISECOND:
							factor *= 1e3;
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
							factor *= 1e3;
						case MICROSECOND:
							factor *= 1e3;
						case MILLISECOND:
							factor *= 1e3;
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
							factor *= 1e3;
						case MICROSECOND:
							factor *= 1e3;
						case MILLISECOND:
							factor *= 1e3;
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
			for ( chop = len; chop>0 && string.charAt(chop-1)=='0'; chop-- ) {}
			int e = len-chop;
			if ( chop>0 && e>2 ) {
				string = string.substring(0, chop) + "e" + e;
			}
			return (reciprocal ? "/" : "*") + string;
		}
		else {
			return "";
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
