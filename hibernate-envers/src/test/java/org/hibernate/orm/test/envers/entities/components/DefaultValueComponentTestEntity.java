/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.components;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Erik-Berndt Scheper
 */
@Entity
@Table(name = "DefaultValueComponent")
@Audited
public class DefaultValueComponentTestEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Embedded
	@Audited
	@AttributeOverrides({@AttributeOverride(name = "comp2.str1", column = @Column(name = "COMP2_STR1"))})
	private DefaultValueComponent1 comp1 = null;

	public DefaultValueComponentTestEntity() {
	}

	public static DefaultValueComponentTestEntity of(
			DefaultValueComponent1 comp1) {
		DefaultValueComponentTestEntity instance = new DefaultValueComponentTestEntity();
		instance.setComp1( comp1 );
		return instance;
	}

	public static DefaultValueComponentTestEntity of(
			Integer id,
			DefaultValueComponent1 comp1) {
		DefaultValueComponentTestEntity instance = new DefaultValueComponentTestEntity();
		instance.setId( id );
		instance.setComp1( comp1 );
		return instance;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public DefaultValueComponent1 getComp1() {
		return comp1;
	}

	public void setComp1(DefaultValueComponent1 comp1) {
		this.comp1 = comp1;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof DefaultValueComponentTestEntity) ) {
			return false;
		}

		DefaultValueComponentTestEntity that = (DefaultValueComponentTestEntity) o;

		if ( comp1 != null ? !comp1.equals( that.comp1 ) : that.comp1 != null ) {
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
		return result;
	}

	public String toString() {
		return "CTE(id = " + id + ", comp1 = " + comp1 + ")";
	}
}
