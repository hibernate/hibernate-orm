/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.manytomany.unidirectional;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.entities.StrTestEntity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class JoinTableEntity implements Serializable {
	@Id
	@GeneratedValue
	private Long id;

	private String data;

	@ManyToMany
	@JoinTable(name = "test_join_table",
			joinColumns = @JoinColumn(name = "assoc_id1"),
			inverseJoinColumns = @JoinColumn(name = "assoc_id2")
	)
	private Set<StrTestEntity> references = new HashSet<StrTestEntity>();

	public JoinTableEntity() {
	}

	public JoinTableEntity(String data) {
		this.data = data;
	}

	public JoinTableEntity(Long id, String data) {
		this.id = id;
		this.data = data;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof JoinTableEntity) ) {
			return false;
		}

		JoinTableEntity that = (JoinTableEntity) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
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

	@Override
	public String toString() {
		return "JoinTableEntity(id = " + id + ", data = " + data + ")";
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set<StrTestEntity> getReferences() {
		return references;
	}

	public void setReferences(Set<StrTestEntity> references) {
		this.references = references;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}
