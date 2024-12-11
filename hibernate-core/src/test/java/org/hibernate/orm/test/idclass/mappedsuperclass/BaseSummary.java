/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idclass.mappedsuperclass;

import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.MappedSuperclass;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@IdClass(PKey.class)
@MappedSuperclass
public class BaseSummary implements Serializable {

	@Id
	private Integer year;
	@Id
	private Integer month;
	private BigDecimal value;

	public Integer getYear() {
		return year;
	}

	public void setYear(Integer year) {
		this.year = year;
	}

	public Integer getMonth() {
		return month;
	}

	public void setMonth(Integer month) {
		this.month = month;
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		BaseSummary that = (BaseSummary) o;
		return Objects.equals( year, that.year ) && Objects.equals( month, that.month );
	}

	@Override
	public int hashCode() {
		return Objects.hash( year, month );
	}
}
