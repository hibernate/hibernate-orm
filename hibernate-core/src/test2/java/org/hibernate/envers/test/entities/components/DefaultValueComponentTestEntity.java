/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
