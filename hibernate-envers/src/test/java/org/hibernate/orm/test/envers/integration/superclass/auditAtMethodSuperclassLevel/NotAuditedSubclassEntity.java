/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.superclass.auditAtMethodSuperclassLevel;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


/**
 * @author Adam Warski (adam at warski dot org)
 * @author Hern&aacute;n Chanfreau
 */
@Entity
@Table(name = "NotAuditedSubclass")
public class NotAuditedSubclassEntity extends AuditedMethodMappedSuperclass {
	@Id
	@GeneratedValue
	private Integer id;

	private String notAuditedStr;

	public NotAuditedSubclassEntity() {
	}

	public NotAuditedSubclassEntity(Integer id, String str, String otherStr, String notAuditedStr) {
		super( str, otherStr );
		this.notAuditedStr = notAuditedStr;
		this.id = id;
	}

	public NotAuditedSubclassEntity(String str, String otherStr, String notAuditedStr) {
		super( str, otherStr );
		this.notAuditedStr = notAuditedStr;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getNotAuditedStr() {
		return notAuditedStr;
	}

	public void setNotAuditedStr(String notAuditedStr) {
		this.notAuditedStr = notAuditedStr;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof NotAuditedSubclassEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		NotAuditedSubclassEntity that = (NotAuditedSubclassEntity) o;

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
