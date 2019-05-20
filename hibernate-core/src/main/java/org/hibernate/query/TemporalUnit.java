/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

/**
 * @author Gavin King
 */
public enum TemporalUnit {
	YEAR, QUARTER, MONTH,
	WEEK, DAY, HOUR, MINUTE,
	SECOND, MILLISECOND, MICROSECOND,
	DAY_OF_WEEK, DAY_OF_YEAR, DAY_OF_MONTH,
	OFFSET, TIMEZONE_HOUR, TIMEZONE_MINUTE;

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}
}
