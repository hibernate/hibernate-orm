/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.accesstype;

import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import jakarta.persistence.Access;
import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class MixedAccessTypeEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Access(AccessType.PROPERTY)
	private String data;

	@Transient
	private boolean dataSet;

	public MixedAccessTypeEntity() {
	}

	public MixedAccessTypeEntity(String data) {
		this.data = data;
	}

	public MixedAccessTypeEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public Integer getId() {
		throw new RuntimeException();
	}

	public void setId(Integer id) {
		throw new RuntimeException();
	}

	// TODO: this should be on the property. But how to discover in AnnotationsMetadataReader that the
	// we should read annotations from fields, even though the access type is "property"?
	@Audited
	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
		dataSet = true;
	}

	public boolean isDataSet() {
		return dataSet;
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
		if ( !(o instanceof MixedAccessTypeEntity) ) {
			return false;
		}

		MixedAccessTypeEntity that = (MixedAccessTypeEntity) o;

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
