/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.superclass.auditAtMethodSuperclassLevel.auditMethodSubclass;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.integration.superclass.auditAtMethodSuperclassLevel.AuditedMethodMappedSuperclass;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Hern&aacut;n Chanfreau
 */
@Entity
@Table(name = "AuditedMethodSubclass")
public class AuditedMethodSubclassEntity extends AuditedMethodMappedSuperclass {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	private String subAuditedStr;


	public AuditedMethodSubclassEntity() {
	}

	public AuditedMethodSubclassEntity(Integer id, String str, String otherString, String subAuditedStr) {
		super( str, otherString );
		this.subAuditedStr = subAuditedStr;
		this.id = id;
	}

	public AuditedMethodSubclassEntity(String str, String otherString, String subAuditedStr) {
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
		if ( !(o instanceof AuditedMethodSubclassEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		AuditedMethodSubclassEntity that = (AuditedMethodSubclassEntity) o;

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
