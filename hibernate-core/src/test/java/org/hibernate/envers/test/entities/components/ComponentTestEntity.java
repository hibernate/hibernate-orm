/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.components;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "CompTest")
public class ComponentTestEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Embedded
	@Audited
	private Component1 comp1;

	@Embedded
	private Component2 comp2;

	public ComponentTestEntity() {
	}

	public ComponentTestEntity(Integer id, Component1 comp1, Component2 comp2) {
		this.id = id;
		this.comp1 = comp1;
		this.comp2 = comp2;
	}

	public ComponentTestEntity(Component1 comp1, Component2 comp2) {
		this.comp1 = comp1;
		this.comp2 = comp2;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Component1 getComp1() {
		return comp1;
	}

	public void setComp1(Component1 comp1) {
		this.comp1 = comp1;
	}

	public Component2 getComp2() {
		return comp2;
	}

	public void setComp2(Component2 comp2) {
		this.comp2 = comp2;
	}

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
		if ( comp2 != null ? !comp2.equals( that.comp2 ) : that.comp2 != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (comp1 != null ? comp1.hashCode() : 0);
		result = 31 * result + (comp2 != null ? comp2.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "CTE(id = " + id + ", comp1 = " + comp1 + ", comp2 = " + comp2 + ")";
	}
}
