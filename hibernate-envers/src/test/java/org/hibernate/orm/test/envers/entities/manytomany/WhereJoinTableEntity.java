/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.manytomany;

import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.entities.IntNoAutoIdTestEntity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class WhereJoinTableEntity {
	@Id
	@GeneratedValue
	private Integer id;

	private String data;

	@ManyToMany
	@JoinTable(
			name = "wjte_ite_join",
			joinColumns = @JoinColumn(name = "wjte_id"),
			inverseJoinColumns = @JoinColumn(name = "ite_id")
	)
	@SQLJoinTableRestriction("ite_id < 20")
	private List<IntNoAutoIdTestEntity> references1;

	@ManyToMany
	@JoinTable(
			name = "wjte_ite_join",
			joinColumns = @JoinColumn(name = "wjte_id"),
			inverseJoinColumns = @JoinColumn(name = "ite_id")
	)
	@SQLJoinTableRestriction("ite_id >= 20")
	private List<IntNoAutoIdTestEntity> references2;

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

	public List<IntNoAutoIdTestEntity> getReferences1() {
		return references1;
	}

	public void setReferences1(List<IntNoAutoIdTestEntity> references1) {
		this.references1 = references1;
	}

	public List<IntNoAutoIdTestEntity> getReferences2() {
		return references2;
	}

	public void setReferences2(List<IntNoAutoIdTestEntity> references2) {
		this.references2 = references2;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		WhereJoinTableEntity that = (WhereJoinTableEntity) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
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
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "WJTE(id = " + id + ", data = " + data + ")";
	}
}
