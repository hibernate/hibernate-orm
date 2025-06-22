/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.manytomany;

import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * Many-to-many not-owning entity
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "ListOwned")
public class ListOwnedEntity {
	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited
	@ManyToMany(mappedBy = "references")
	private List<ListOwningEntity> referencing;

	public ListOwnedEntity() {
	}

	public ListOwnedEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public ListOwnedEntity(String data) {
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

	public List<ListOwningEntity> getReferencing() {
		return referencing;
	}

	public void setReferencing(List<ListOwningEntity> referencing) {
		this.referencing = referencing;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ListOwnedEntity) ) {
			return false;
		}

		ListOwnedEntity that = (ListOwnedEntity) o;

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
		return "ListOwnedEntity(id = " + id + ", data = " + data + ")";
	}
}
