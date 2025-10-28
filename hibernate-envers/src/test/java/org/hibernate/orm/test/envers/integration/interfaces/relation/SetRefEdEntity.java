/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.interfaces.relation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * ReferencEd entity
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class SetRefEdEntity implements ISetRefEdEntity {
	@Id
	private Integer id;

	private String data;

	public SetRefEdEntity() {
	}

	public SetRefEdEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public SetRefEdEntity(String data) {
		this.data = data;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof SetRefEdEntity) ) {
			return false;
		}

		SetRefEdEntity that = (SetRefEdEntity) o;

		if ( data != null ? !data.equals( that.getData() ) : that.getData() != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.getId() ) : that.getId() != null ) {
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

	public String toString() {
		return "SetRefEdEntity(id = " + id + ", data = " + data + ")";
	}
}
