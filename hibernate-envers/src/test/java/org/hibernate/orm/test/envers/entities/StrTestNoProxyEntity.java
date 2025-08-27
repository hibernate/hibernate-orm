/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities;

import java.io.Serializable;

import org.hibernate.envers.Audited;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Table(name = "STR_TEST_NP")
public class StrTestNoProxyEntity implements Serializable {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	private String str;

	public StrTestNoProxyEntity() {
	}

	public StrTestNoProxyEntity(String str, Integer id) {
		this.str = str;
		this.id = id;
	}

	public StrTestNoProxyEntity(String str) {
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
		if ( !(o instanceof StrTestNoProxyEntity) ) {
			return false;
		}

		StrTestNoProxyEntity that = (StrTestNoProxyEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( str != null ? !str.equals( that.str ) : that.str != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (str != null ? str.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "STNPE(id = " + id + ", str = " + str + ")";
	}
}
