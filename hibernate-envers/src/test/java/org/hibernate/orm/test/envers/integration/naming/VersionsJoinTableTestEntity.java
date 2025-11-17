/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.naming;

import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.entities.StrTestEntity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "VersionsJoinTable")
public class VersionsJoinTableTestEntity {
	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited
	@OneToMany
	@JoinColumn(name = "VJT_ID")
	@AuditJoinTable(name = "VERSIONS_JOIN_TABLE_TEST", inverseJoinColumns = @JoinColumn(name = "STR_ID"))
	private Set<StrTestEntity> collection;

	public VersionsJoinTableTestEntity() {
	}

	public VersionsJoinTableTestEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public VersionsJoinTableTestEntity(String data) {
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

	public Set<StrTestEntity> getCollection() {
		return collection;
	}

	public void setCollection(Set<StrTestEntity> collection) {
		this.collection = collection;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof VersionsJoinTableTestEntity) ) {
			return false;
		}

		VersionsJoinTableTestEntity that = (VersionsJoinTableTestEntity) o;

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
		return "VersionsJoinTableTestEntity(id = " + id + ", data = " + data + ")";
	}
}
