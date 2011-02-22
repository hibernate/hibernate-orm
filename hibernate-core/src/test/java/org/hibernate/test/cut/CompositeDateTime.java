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
package org.hibernate.test.cut;

import java.io.Serializable;

/**
 * Class for testing composite user types with more than two fields.
 *
 * @author Etienne Miret
 */
public class CompositeDateTime implements Serializable {

	private static final long serialVersionUID = 7401750071679578453L;

	private Integer year;

	private Integer month;

	private Integer day;

	private Integer hour;

	private Integer minute;

	private Integer second;

	public CompositeDateTime(final Integer year, final Integer month, final Integer day, final Integer hour,
			final Integer minute, final Integer second) {
		super();
		this.year = year;
		this.month = month;
		this.day = day;
		this.hour = hour;
		this.minute = minute;
		this.second = second;
	}

	/*
	 * Constructor for those who hate auto (un)boxing.
	 */
	public CompositeDateTime(final int year, final int month, final int day, final int hour,
			final int minute, final int second) {
		this( new Integer( year ), Integer.valueOf( month ), Integer.valueOf( day ), Integer.valueOf( hour ),
				Integer.valueOf( minute ), Integer.valueOf( second ) );
	}

	public CompositeDateTime(final CompositeDateTime other) {
		super();
		this.year = other.year;
		this.month = other.month;
		this.day = other.day;
		this.hour = other.hour;
		this.minute = other.minute;
		this.second = other.second;
	}

	public Integer getYear() {
		return year;
	}

	public void setYear(final Integer year) {
		this.year = year;
	}

	public Integer getMonth() {
		return month;
	}

	public void setMonth(final Integer month) {
		this.month = month;
	}

	public Integer getDay() {
		return day;
	}

	public void setDay(final Integer day) {
		this.day = day;
	}

	public Integer getHour() {
		return hour;
	}

	public void setHour(final Integer hour) {
		this.hour = hour;
	}

	public Integer getMinute() {
		return minute;
	}

	public void setMinute(final Integer minute) {
		this.minute = minute;
	}

	public Integer getSecond() {
		return second;
	}

	public void setSecond(final Integer second) {
		this.second = second;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( day == null ) ? 0 : day.hashCode() );
		result = prime * result + ( ( hour == null ) ? 0 : hour.hashCode() );
		result = prime * result + ( ( minute == null ) ? 0 : minute.hashCode() );
		result = prime * result + ( ( month == null ) ? 0 : month.hashCode() );
		result = prime * result + ( ( second == null ) ? 0 : second.hashCode() );
		result = prime * result + ( ( year == null ) ? 0 : year.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( !( obj instanceof CompositeDateTime ) ) {
			return false;
		}
		CompositeDateTime other = (CompositeDateTime) obj;
		if ( day == null ) {
			if ( other.day != null ) {
				return false;
			}
		}
		else if ( !day.equals( other.day ) ) {
			return false;
		}
		if ( hour == null ) {
			if ( other.hour != null ) {
				return false;
			}
		}
		else if ( !hour.equals( other.hour ) ) {
			return false;
		}
		if ( minute == null ) {
			if ( other.minute != null ) {
				return false;
			}
		}
		else if ( !minute.equals( other.minute ) ) {
			return false;
		}
		if ( month == null ) {
			if ( other.month != null ) {
				return false;
			}
		}
		else if ( !month.equals( other.month ) ) {
			return false;
		}
		if ( second == null ) {
			if ( other.second != null ) {
				return false;
			}
		}
		else if ( !second.equals( other.second ) ) {
			return false;
		}
		if ( year == null ) {
			if ( other.year != null ) {
				return false;
			}
		}
		else if ( !year.equals( other.year ) ) {
			return false;
		}
		return true;
	}

}
