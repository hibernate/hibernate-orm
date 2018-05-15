/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.components.relations;

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
@Table(name = "NotAudM2OCompEnt")
public class NotAuditedManyToOneComponentTestEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Embedded
	@Audited
	private NotAuditedManyToOneComponent comp1;

	public NotAuditedManyToOneComponentTestEntity() {
	}

	public NotAuditedManyToOneComponentTestEntity(Integer id, NotAuditedManyToOneComponent comp1) {
		this.id = id;
		this.comp1 = comp1;
	}

	public NotAuditedManyToOneComponentTestEntity(NotAuditedManyToOneComponent comp1) {
		this.comp1 = comp1;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public NotAuditedManyToOneComponent getComp1() {
		return comp1;
	}

	public void setComp1(NotAuditedManyToOneComponent comp1) {
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

		NotAuditedManyToOneComponentTestEntity that = (NotAuditedManyToOneComponentTestEntity) o;

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
		return "NAMTOCTE(id = " + id + ", comp1 = " + comp1 + ")";
	}
}