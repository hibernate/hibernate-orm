/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.components;

import java.util.Objects;

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

	private Integer id;


	private Component1 comp1;


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

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Embedded
	@Audited
	public Component1 getComp1() {
		return comp1;
	}

	public void setComp1(Component1 comp1) {
		this.comp1 = comp1;
	}

	@Embedded
	public Component2 getComp2() {
		return comp2;
	}

	public void setComp2(Component2 comp2) {
		this.comp2 = comp2;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ComponentTestEntity that = (ComponentTestEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( comp1, that.comp1 ) &&
				Objects.equals( comp2, that.comp2 );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, comp1, comp2 );
	}

	@Override
	public String toString() {
		return "ComponentTestEntity{" +
				"id=" + id +
				", comp1=" + comp1 +
				", comp2=" + comp2 +
				'}';
	}
}
