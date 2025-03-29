/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.manytomany.sametable;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class Child2Entity {
	@Id
	@GeneratedValue
	private Integer id;

	private String child2Data;

	public Child2Entity() {
	}

	public Child2Entity(String child2Data) {
		this.child2Data = child2Data;
	}

	public Child2Entity(Integer id, String child2Data) {
		this.id = id;
		this.child2Data = child2Data;
	}

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "children",
			joinColumns = @JoinColumn(name = "child2_id"),
			inverseJoinColumns = @JoinColumn(name = "parent_id", insertable = false, updatable = false)
	)
	@SQLJoinTableRestriction("child2_id is not null")
	private List<ParentEntity> parents = new ArrayList<ParentEntity>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getChild2Data() {
		return child2Data;
	}

	public void setChild2Data(String child2Data) {
		this.child2Data = child2Data;
	}

	public List<ParentEntity> getParents() {
		return parents;
	}

	public void setParents(List<ParentEntity> parents) {
		this.parents = parents;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Child2Entity that = (Child2Entity) o;

		if ( child2Data != null ? !child2Data.equals( that.child2Data ) : that.child2Data != null ) {
			return false;
		}
		//noinspection RedundantIfStatement
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (child2Data != null ? child2Data.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "C2E(id = " + id + ", child2Data = " + child2Data + ")";
	}
}
