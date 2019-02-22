/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.basic;

import java.util.Date;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class DateTestEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	private Date dateValue;

	public DateTestEntity() {
	}

	public DateTestEntity(Date dateValue) {
		this.dateValue = dateValue;
	}

	public DateTestEntity(Integer id, Date date) {
		this.id = id;
		this.dateValue = date;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Date getDateValue() {
		return dateValue;
	}

	public void setDateValue(Date dateValue) {
		this.dateValue = dateValue;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		DateTestEntity that = (DateTestEntity) o;

		if ( dateValue != null ) {
			if ( that.dateValue == null ) {
				return false;
			}
			if ( dateValue.getTime() != that.dateValue.getTime() ) {
				return false;
			}
		}
		return Objects.equals( id, that.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, dateValue );
	}

	@Override
	public String toString() {
		return "DateTestEntity{" +
				"id=" + id +
				", dateValue=" + dateValue +
				'}';
	}
}