/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.collection;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.components.Component3;

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