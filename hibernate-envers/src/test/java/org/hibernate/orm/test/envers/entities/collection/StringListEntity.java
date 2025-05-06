/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.collection;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.Audited;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class StringListEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@ElementCollection
	@OrderColumn(name = "list_index")
	private List<String> strings;

	public StringListEntity() {
		strings = new ArrayList<String>();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public List<String> getStrings() {
		return strings;
	}

	public void setStrings(List<String> strings) {
		this.strings = strings;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof StringListEntity) ) {
			return false;
		}

		StringListEntity that = (StringListEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}

	public String toString() {
		return "SLE(id = " + id + ", strings = " + strings + ")";
	}
}
