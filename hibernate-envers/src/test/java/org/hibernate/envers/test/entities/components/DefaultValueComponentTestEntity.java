/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.entities.components;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

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
