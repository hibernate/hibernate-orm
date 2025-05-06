/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.accesstype;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class FieldAccessTypeEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	private String data;

	public FieldAccessTypeEntity() {
	}

	public FieldAccessTypeEntity(String data) {
		this.data = data;
	}

	public FieldAccessTypeEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public Integer getId() {
		throw new RuntimeException();
	}

	public void setId(Integer id) {
		throw new RuntimeException();
	}

	public String getData() {
		throw new RuntimeException();
	}

	public void setData(String data) {
		throw new RuntimeException();
	}

	public Integer readId() {
		return id;
	}

	public void writeData(String data) {
		this.data = data;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof FieldAccessTypeEntity) ) {
			return false;
		}

		FieldAccessTypeEntity that = (FieldAccessTypeEntity) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}
}
