/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.collection;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.entities.components.relations.ManyToOneEagerComponent;

/**
 * Embeddable list with components encapsulating many-to-one relation (referencing some entity).
 *
 * @author thiagolrc
 */
@Entity
@Table(name = "EmbListEnt2")
@Audited
public class EmbeddableListEntity2 {
	@Id
	@GeneratedValue
	private Integer id;

	@ElementCollection
	@OrderColumn
	@CollectionTable(name = "EmbListEnt2_list")
	private List<ManyToOneEagerComponent> componentList = new ArrayList<ManyToOneEagerComponent>();

	public EmbeddableListEntity2() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public List<ManyToOneEagerComponent> getComponentList() {
		return componentList;
	}

	public void setComponentList(List<ManyToOneEagerComponent> componentList) {
		this.componentList = componentList;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof EmbeddableListEntity2) ) {
			return false;
		}

		EmbeddableListEntity2 that = (EmbeddableListEntity2) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}

	public String toString() {
		return "ELE2(id = " + id + ", componentList = " + componentList + ")";
	}
}
