/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.manytomany.unidirectional;

import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.entities.StrTestEntity;

/**
 * Entity owning the many-to-many relation
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class ListUniEntity {
	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited
	@ManyToMany
	private List<StrTestEntity> references;

	public ListUniEntity() {
	}

	public ListUniEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public ListUniEntity(String data) {
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

	public List<StrTestEntity> getReferences() {
		return references;
	}

	public void setReferences(List<StrTestEntity> references) {
		this.references = references;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ListUniEntity) ) {
			return false;
		}

		ListUniEntity that = (ListUniEntity) o;

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
		return "ListUniEntity(id = " + id + ", data = " + data + ")";
	}
}
