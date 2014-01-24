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
package org.hibernate.envers.test.integration.customtype;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import java.io.Serializable;
import java.util.Calendar;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.envers.Audited;
import org.hibernate.type.CalendarDateType;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
@TypeDef(name = "hibernate_calendar_date", typeClass = CalendarDateType.class)
public class CalendarEntity implements Serializable {
	@Id
	@GeneratedValue
	private Long id = null;

	@Type(type = "hibernate_calendar_date")
	private Calendar dayOne = Calendar.getInstance(); // org.hibernate.type.CalendarDateType

	private Calendar dayTwo = Calendar.getInstance(); // org.hibernate.envers.test.integration.customtype.UTCCalendarType

	public CalendarEntity() {
	}

	public CalendarEntity(Calendar dayOne, Calendar dayTwo) {
		this.dayOne = dayOne;
		this.dayTwo = dayTwo;
	}

	public CalendarEntity(Long id, Calendar dayOne, Calendar dayTwo) {
		this.id = id;
		this.dayOne = dayOne;
		this.dayTwo = dayTwo;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof CalendarEntity ) ) return false;

		CalendarEntity that = (CalendarEntity) o;

		if ( dayOne != null ? !dayOne.equals( that.dayOne ) : that.dayOne != null ) return false;
		if ( dayTwo != null ? !dayTwo.equals( that.dayTwo ) : that.dayTwo != null ) return false;
		if ( id != null ? !id.equals( that.id ) : that.id != null ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (dayOne != null ? dayOne.hashCode() : 0);
		result = 31 * result + (dayTwo != null ? dayTwo.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "CalendarEntity(id = " + id + ", dayOne = " + dayOne + ", dayTwo = " + dayTwo + ")";
	}

	public Calendar getDayOne() {
		return dayOne;
	}

	public void setDayOne(Calendar dayOne) {
		this.dayOne = dayOne;
	}

	public Calendar getDayTwo() {
		return dayTwo;
	}

	public void setDayTwo(Calendar dayTwo) {
		this.dayTwo = dayTwo;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
