/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class StrIntTestEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	private String str1;

	@Audited
	@Column(name = "NUM_VAL")
	private Integer number;

	public StrIntTestEntity() {
	}

	public StrIntTestEntity(String str1, Integer number, Integer id) {
		this.id = id;
		this.str1 = str1;
		this.number = number;
	}

	public StrIntTestEntity(String str1, Integer number) {
		this.str1 = str1;
		this.number = number;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getStr1() {
		return str1;
	}

	public void setStr1(String str1) {
		this.str1 = str1;
	}

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof StrIntTestEntity) ) {
			return false;
		}

		StrIntTestEntity that = (StrIntTestEntity) o;

		if ( id != null ? !id.equals( that.getId() ) : that.getId() != null ) {
			return false;
		}
		if ( number != null ? !number.equals( that.getNumber() ) : that.getNumber() != null ) {
			return false;
		}
		if ( str1 != null ? !str1.equals( that.getStr1() ) : that.getStr1() != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (str1 != null ? str1.hashCode() : 0);
		result = 31 * result + (number != null ? number.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "SITE(id = " + id + ", str1 = " + str1 + ", number = " + number + ")";
	}
}
