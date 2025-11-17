/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.accesstype;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class PropertyAccessTypeEntity {
	private Integer id;
	private String data;

	private boolean idSet;
	private boolean dataSet;

	public PropertyAccessTypeEntity() {
	}

	public PropertyAccessTypeEntity(String data) {
		this.data = data;
	}

	public PropertyAccessTypeEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
		idSet = true;
	}

	@Audited
	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
		dataSet = true;
	}

	@Transient
	public boolean isIdSet() {
		return idSet;
	}

	@Transient
	public boolean isDataSet() {
		return dataSet;
	}

	public void writeData(String data) {
		this.data = data;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof PropertyAccessTypeEntity) ) {
			return false;
		}

		PropertyAccessTypeEntity that = (PropertyAccessTypeEntity) o;

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
