/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "UNVER_DATA")
public class UnversionedStrTestEntity {
	@Id
	@GeneratedValue
	private Integer id;

	private String str;

	public UnversionedStrTestEntity() {
	}

	public UnversionedStrTestEntity(String str, Integer id) {
		this.str = str;
		this.id = id;
	}

	public UnversionedStrTestEntity(String str) {
		this.str = str;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof UnversionedStrTestEntity) ) {
			return false;
		}

		UnversionedStrTestEntity that = (UnversionedStrTestEntity) o;

		if ( id != null ? !id.equals( that.getId() ) : that.getId() != null ) {
			return false;
		}
		if ( str != null ? !str.equals( that.getStr() ) : that.getStr() != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (str != null ? str.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "USTE(id = " + id + ", str = " + str + ")";
	}
}
