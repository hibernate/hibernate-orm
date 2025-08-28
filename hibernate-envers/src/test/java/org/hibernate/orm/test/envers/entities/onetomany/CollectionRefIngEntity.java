/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.onetomany;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.envers.Audited;

/**
 * ReferencIng entity
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class CollectionRefIngEntity implements Serializable {
	private static final long serialVersionUID = -9019967223928425707L;

	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited
	@ManyToOne
	private CollectionRefEdEntity reference;

	public CollectionRefIngEntity() {
	}

	public CollectionRefIngEntity(Integer id, String data, CollectionRefEdEntity reference) {
		this.id = id;
		this.data = data;
		this.reference = reference;
	}

	public CollectionRefIngEntity(Integer id, String data) {
		this.id = id;
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

	public CollectionRefEdEntity getReference() {
		return reference;
	}

	public void setReference(CollectionRefEdEntity reference) {
		this.reference = reference;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof CollectionRefIngEntity) ) {
			return false;
		}

		CollectionRefIngEntity that = (CollectionRefIngEntity) o;

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

	public String toString() {
		return "CollectionRefIngEntity(id = " + id + ", data = " + data + ")";
	}
}
