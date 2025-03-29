/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.manytomany.biowned;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import org.hibernate.envers.Audited;

/**
 * Entity owning a many-to-many relation, where the other entity also owns the relation.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class ListBiowning2Entity {
	@Id
	@GeneratedValue
	private Integer id;

	private String data;

	@ManyToMany
	@JoinTable(
			name = "biowning",
			joinColumns = @JoinColumn(name = "biowning2_id"),
			inverseJoinColumns = @JoinColumn(name = "biowning1_id", insertable = false, updatable = false)
	)
	private List<ListBiowning1Entity> references = new ArrayList<ListBiowning1Entity>();

	public ListBiowning2Entity() {
	}

	public ListBiowning2Entity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public ListBiowning2Entity(String data) {
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

	public List<ListBiowning1Entity> getReferences() {
		return references;
	}

	public void setReferences(List<ListBiowning1Entity> references) {
		this.references = references;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ListBiowning2Entity) ) {
			return false;
		}

		ListBiowning2Entity that = (ListBiowning2Entity) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		//noinspection RedundantIfStatement
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
		return "ListBiowning2Entity(id = " + id + ", data = " + data + ")";
	}
}
