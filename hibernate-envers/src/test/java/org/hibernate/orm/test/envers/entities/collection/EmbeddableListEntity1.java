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
import org.hibernate.orm.test.envers.entities.components.Component3;

/**
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 */
@Entity
@Table(name = "EmbListEnt1")
@Audited
public class EmbeddableListEntity1 {
	@Id
	@GeneratedValue
	private Integer id;

	private String otherData;

	@ElementCollection
	@OrderColumn
	@CollectionTable(name = "EmbListEnt1_list")
	private List<Component3> componentList = new ArrayList<Component3>();

	public EmbeddableListEntity1() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public List<Component3> getComponentList() {
		return componentList;
	}

	public void setComponentList(List<Component3> componentList) {
		this.componentList = componentList;
	}

	public String getOtherData() {
		return otherData;
	}

	public void setOtherData(String otherData) {
		this.otherData = otherData;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof EmbeddableListEntity1) ) {
			return false;
		}

		EmbeddableListEntity1 that = (EmbeddableListEntity1) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}

	public String toString() {
		return "ELE1(id = " + id + ", otherData = " + otherData + ", componentList = " + componentList + ")";
	}
}
