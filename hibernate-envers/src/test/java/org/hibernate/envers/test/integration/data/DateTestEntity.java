/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.data;

import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

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

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof DateTestEntity) ) {
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

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (dateValue != null ? dateValue.hashCode() : 0);
		return result;
	}
}
