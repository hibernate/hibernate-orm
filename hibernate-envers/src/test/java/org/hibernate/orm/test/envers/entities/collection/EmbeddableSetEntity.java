/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.collection;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.entities.components.Component3;

/**
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 */
@Entity
@Table(name = "EmbSetEnt")
@Audited
public class EmbeddableSetEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@ElementCollection
	@CollectionTable(name = "EmbSetEnt_set")
	private Set<Component3> componentSet = new HashSet<Component3>();

	public EmbeddableSetEntity() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<Component3> getComponentSet() {
		return componentSet;
	}

	public void setComponentSet(Set<Component3> componentSet) {
		this.componentSet = componentSet;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof EmbeddableSetEntity) ) {
			return false;
		}

		EmbeddableSetEntity that = (EmbeddableSetEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}

	public String toString() {
		return "ESE(id = " + id + ", componentSet = " + componentSet + ')';
	}
}
