/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.manytomany;

import java.util.Set;
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
@Table(name = "MapOwned")
public class MapOwnedEntity {
	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited
	@ManyToMany(mappedBy = "references")
	private Set<MapOwningEntity> referencing;

	public MapOwnedEntity() {
	}

	public MapOwnedEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public MapOwnedEntity(String data) {
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

	public Set<MapOwningEntity> getReferencing() {
		return referencing;
	}

	public void setReferencing(Set<MapOwningEntity> referencing) {
		this.referencing = referencing;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof MapOwnedEntity) ) {
			return false;
		}

		MapOwnedEntity that = (MapOwnedEntity) o;

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
		return "MapOwnedEntity(id = " + id + ", data = " + data + ")";
	}
}
