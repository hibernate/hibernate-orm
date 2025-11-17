/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.collection;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class StringSetEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@ElementCollection
	private Set<String> strings;

	public StringSetEntity() {
		strings = new HashSet<String>();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<String> getStrings() {
		return strings;
	}

	public void setStrings(Set<String> strings) {
		this.strings = strings;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof StringSetEntity) ) {
			return false;
		}

		StringSetEntity that = (StringSetEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}

	public String toString() {
		return "SSE(id = " + id + ", strings = " + strings + ")";
	}
}
