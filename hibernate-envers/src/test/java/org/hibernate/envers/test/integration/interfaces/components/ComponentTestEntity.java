/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.interfaces.components;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Target;
import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class ComponentTestEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Embedded
	@Target(Component1.class)
	private IComponent comp1;

	public ComponentTestEntity() {
	}

	public ComponentTestEntity(IComponent comp1) {
		this.comp1 = comp1;
	}

	public ComponentTestEntity(Integer id, IComponent comp1) {
		this.id = id;
		this.comp1 = comp1;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public IComponent getComp1() {
		return comp1;
	}

	public void setComp1(IComponent comp1) {
		this.comp1 = comp1;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ComponentTestEntity) ) {
			return false;
		}

		ComponentTestEntity that = (ComponentTestEntity) o;

		if ( comp1 != null ? !comp1.equals( that.comp1 ) : that.comp1 != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (comp1 != null ? comp1.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ComponentTestEntity{" +
				"id=" + id +
				", comp1=" + comp1 +
				'}';
	}
}