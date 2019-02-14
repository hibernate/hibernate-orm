/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.collections.embeddable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.support.domains.components.PartialAuditedComponent;

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
	private List<PartialAuditedComponent> componentList = new ArrayList<>();

	public EmbeddableListEntity1() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public List<PartialAuditedComponent> getComponentList() {
		return componentList;
	}

	public void setComponentList(List<PartialAuditedComponent> componentList) {
		this.componentList = componentList;
	}

	public String getOtherData() {
		return otherData;
	}

	public void setOtherData(String otherData) {
		this.otherData = otherData;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		EmbeddableListEntity1 that = (EmbeddableListEntity1) o;
		return Objects.equals( id, that.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id );
	}

	@Override
	public String toString() {
		return "EmbeddableListEntity1{" +
				"id=" + id +
				", otherData='" + otherData + '\'' +
				", componentList=" + componentList +
				'}';
	}
}