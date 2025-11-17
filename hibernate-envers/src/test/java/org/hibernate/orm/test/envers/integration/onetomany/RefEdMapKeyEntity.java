/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class RefEdMapKeyEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@OneToMany(mappedBy = "reference")
	@MapKey(name = "data")
	private Map<String, RefIngMapKeyEntity> idmap;

	public RefEdMapKeyEntity() {
		idmap = new HashMap<String, RefIngMapKeyEntity>();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Map<String, RefIngMapKeyEntity> getIdmap() {
		return idmap;
	}

	public void setIdmap(Map<String, RefIngMapKeyEntity> idmap) {
		this.idmap = idmap;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof RefEdMapKeyEntity) ) {
			return false;
		}

		RefEdMapKeyEntity that = (RefEdMapKeyEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}

	public String toString() {
		return "RedMKE(id = " + id + ", idmap = " + idmap + ")";
	}
}
