/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.components.relations;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;

import org.hibernate.orm.test.envers.entities.StrTestEntity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Embeddable
public class OneToManyComponent {
	@OneToMany
	@JoinTable(joinColumns = @JoinColumn(name = "OneToMany_id"))
	private Set<StrTestEntity> entities = new HashSet<StrTestEntity>();

	private String data;

	public OneToManyComponent(String data) {
		this.data = data;
	}

	public OneToManyComponent() {
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public Set<StrTestEntity> getEntities() {
		return entities;
	}

	public void setEntities(Set<StrTestEntity> entities) {
		this.entities = entities;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		OneToManyComponent that = (OneToManyComponent) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		if ( entities != null ? !entities.equals( that.entities ) : that.entities != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = entities != null ? entities.hashCode() : 0;
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "OneToManyComponent(data = " + data + ")";
	}
}
