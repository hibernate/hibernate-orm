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
public class ParentEntity {
	@Id
	@GeneratedValue
	private Integer id;

	private String parentData;

	public ParentEntity() {
	}

	public ParentEntity(String parentData) {
		this.parentData = parentData;
	}

	public ParentEntity(Integer id, String parentData) {
		this.id = id;
		this.parentData = parentData;
	}

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "children",
			joinColumns = @JoinColumn(name = "parent_id"),
			inverseJoinColumns = @JoinColumn(name = "child1_id", insertable = false, updatable = false)
	)
	@SQLJoinTableRestriction("child1_id is not null")
	private List<Child1Entity> children1 = new ArrayList<Child1Entity>();

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "children",
			joinColumns = @JoinColumn(name = "parent_id"),
			inverseJoinColumns = @JoinColumn(name = "child2_id", insertable = false, updatable = false)
	)
	@SQLJoinTableRestriction("child2_id is not null")
	private List<Child2Entity> children2 = new ArrayList<Child2Entity>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getParentData() {
		return parentData;
	}

	public void setParentData(String parentData) {
		this.parentData = parentData;
	}

	public List<Child1Entity> getChildren1() {
		return children1;
	}

	public void setChildren1(List<Child1Entity> children1) {
		this.children1 = children1;
	}

	public List<Child2Entity> getChildren2() {
		return children2;
	}

	public void setChildren2(List<Child2Entity> children2) {
		this.children2 = children2;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		ParentEntity that = (ParentEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		//noinspection RedundantIfStatement
		if ( parentData != null ? !parentData.equals( that.parentData ) : that.parentData != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (parentData != null ? parentData.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "PE(id = " + id + ", parentData = " + parentData + ")";
	}
}
