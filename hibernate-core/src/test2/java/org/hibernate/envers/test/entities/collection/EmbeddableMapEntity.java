/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.collection;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.components.Component3;

/**
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 */
@Entity
@Table(name = "EmbMapEnt")
public class EmbeddableMapEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@ElementCollection
	@CollectionTable(name = "EmbMapEnt_map")
	@MapKeyColumn(nullable = false) // NOT NULL for Sybase
	private Map<String, Component3> componentMap;

	public EmbeddableMapEntity() {
		componentMap = new HashMap<String, Component3>();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Map<String, Component3> getComponentMap() {
		return componentMap;
	}

	public void setComponentMap(Map<String, Component3> strings) {
		this.componentMap = strings;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof EmbeddableMapEntity) ) {
			return false;
		}

		EmbeddableMapEntity that = (EmbeddableMapEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}

	public String toString() {
		return "EME(id = " + id + ", componentMap = " + componentMap + ")";
	}
}