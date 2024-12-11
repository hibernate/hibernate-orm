/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idclass.mappedsuperclass;

import java.io.Serializable;
import java.util.Objects;

public class PKey implements Serializable {

	private Integer year;
	private Integer month;

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

	@Override
	public boolean equals(Object o) {
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		PKey pKey = (PKey) o;
		return Objects.equals( year, pKey.year ) && Objects.equals( month, pKey.month );
	}

	@Override
	public int hashCode() {
		return Objects.hash( year, month );
	}
}
