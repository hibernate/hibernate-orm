/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.collection;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.components.relations.ManyToOneEagerComponent;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity with an List of Embeddable Components that have ManyToOne relationships
 *
 * @author Cankut Guven
 */
@Entity
@Table(name = "EmbListEnt3")
@Audited
public class EmbeddableListEntity3 {
	@Id
	@GeneratedValue
	private Integer id;

	@ElementCollection
	@OrderColumn
	@CollectionTable(name = "EmbListEnt3_list")
	private List<ManyToOneEagerComponent> componentList = new ArrayList<ManyToOneEagerComponent>();

	public EmbeddableListEntity3() {
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
		if ( !(o instanceof EmbeddableListEntity3) ) {
			return false;
		}

		EmbeddableListEntity3 that = (EmbeddableListEntity3) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}

	public String toString() {
		return "ELE3(id = " + id + ", componentList = " + componentList + ")";
	}
}
