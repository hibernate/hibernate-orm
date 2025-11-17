/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.components.relations;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "ManyToOneCompEnt")
public class ManyToOneComponentTestEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Embedded
	@Audited
	private ManyToOneComponent comp1;

	public ManyToOneComponentTestEntity() {
	}

	public ManyToOneComponentTestEntity(Integer id, ManyToOneComponent comp1) {
		this.id = id;
		this.comp1 = comp1;
	}

	public ManyToOneComponentTestEntity(ManyToOneComponent comp1) {
		this.comp1 = comp1;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public ManyToOneComponent getComp1() {
		return comp1;
	}

	public void setComp1(ManyToOneComponent comp1) {
		this.comp1 = comp1;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		ManyToOneComponentTestEntity that = (ManyToOneComponentTestEntity) o;

		if ( comp1 != null ? !comp1.equals( that.comp1 ) : that.comp1 != null ) {
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
		result = 31 * result + (comp1 != null ? comp1.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "MTOCTE(id = " + id + ", comp1 = " + comp1 + ")";
	}
}
