/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.superclass.auditAtMethodSuperclassLevel.auditAllSubclass;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.integration.superclass.auditAtMethodSuperclassLevel.AuditedMethodMappedSuperclass;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Hern&aacut;n Chanfreau
 */
@Entity
@Audited
public class AuditedAllSubclassEntity extends AuditedMethodMappedSuperclass {
	@Id
	@GeneratedValue
	private Integer id;

	private String subAuditedStr;


	public AuditedAllSubclassEntity() {
	}

	public AuditedAllSubclassEntity(Integer id, String str, String otherString, String subAuditedStr) {
		super( str, otherString );
		this.subAuditedStr = subAuditedStr;
		this.id = id;
	}

	public AuditedAllSubclassEntity(String str, String otherString, String subAuditedStr) {
		super( str, otherString );
		this.subAuditedStr = subAuditedStr;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getSubAuditedStr() {
		return subAuditedStr;
	}

	public void setSubAuditedStr(String subAuditedStr) {
		this.subAuditedStr = subAuditedStr;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof AuditedAllSubclassEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		AuditedAllSubclassEntity that = (AuditedAllSubclassEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (id != null ? id.hashCode() : 0);
		return result;
	}
}
