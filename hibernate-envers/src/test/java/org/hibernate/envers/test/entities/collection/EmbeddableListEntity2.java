/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.entities.collection;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.components.relations.ManyToOneEagerComponent;

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