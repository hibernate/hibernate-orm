/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.bidirectional;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class BiRefEdEntity {
	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited
	@OneToOne(mappedBy = "reference")
	private BiRefIngEntity referencing;

	public BiRefEdEntity() {
	}

	public BiRefEdEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public BiRefEdEntity(Integer id, String data, BiRefIngEntity referencing) {
		this.id = id;
		this.data = data;
		this.referencing = referencing;
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

	public BiRefIngEntity getReferencing() {
		return referencing;
	}

	public void setReferencing(BiRefIngEntity referencing) {
		this.referencing = referencing;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof BiRefEdEntity) ) {
			return false;
		}

		BiRefEdEntity that = (BiRefEdEntity) o;

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
