/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.components;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class ComponentSetTestEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@ElementCollection
	@CollectionTable(name = "CompTestEntityComps", joinColumns = @JoinColumn(name = "entity_id"))
	private Set<Component1> comps = new HashSet<Component1>();

	public ComponentSetTestEntity() {
	}

	public ComponentSetTestEntity(Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<Component1> getComps() {
		return comps;
	}

	public void setComps(Set<Component1> comps) {
		this.comps = comps;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ComponentSetTestEntity) ) {
			return false;
		}

		ComponentSetTestEntity that = (ComponentSetTestEntity) o;

		if ( comps != null ? !comps.equals( that.comps ) : that.comps != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (comps != null ? comps.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ComponentSetTestEntity{" +
				"id=" + id +
				", comps=" + comps +
				'}';
	}
}