/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.interfaces.components;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.TargetEmbeddable;
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
	@TargetEmbeddable(Component1.class)
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
