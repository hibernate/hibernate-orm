/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection.mapkey;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MapKey;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.components.Component1;
import org.hibernate.envers.test.entities.components.ComponentTestEntity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "CompMapKey")
public class ComponentMapKeyEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@ManyToMany
	@MapKey(name = "comp1")
	private Map<Component1, ComponentTestEntity> idmap;

	public ComponentMapKeyEntity() {
		idmap = new HashMap<Component1, ComponentTestEntity>();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Map<Component1, ComponentTestEntity> getIdmap() {
		return idmap;
	}

	public void setIdmap(Map<Component1, ComponentTestEntity> idmap) {
		this.idmap = idmap;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ComponentMapKeyEntity) ) {
			return false;
		}

		ComponentMapKeyEntity that = (ComponentMapKeyEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}

	public String toString() {
		return "CMKE(id = " + id + ", idmap = " + idmap + ")";
	}
}