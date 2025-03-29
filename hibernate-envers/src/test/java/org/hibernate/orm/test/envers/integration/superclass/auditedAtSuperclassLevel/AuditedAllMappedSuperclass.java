/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.superclass.auditedAtSuperclassLevel;

import jakarta.persistence.MappedSuperclass;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Hern&aacut;n Chanfreau
 */
@MappedSuperclass
@Audited
public class AuditedAllMappedSuperclass {

	private String str;

	private String otherStr;

	public AuditedAllMappedSuperclass() {
	}

	public AuditedAllMappedSuperclass(String str, String otherStr) {
		this.str = str;
		this.otherStr = otherStr;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public String getOtherStr() {
		return otherStr;
	}

	public void setOtherStr(String otherStr) {
		this.otherStr = otherStr;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof AuditedAllMappedSuperclass) ) {
			return false;
		}

		AuditedAllMappedSuperclass that = (AuditedAllMappedSuperclass) o;

		if ( str != null ? !str.equals( that.str ) : that.str != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return (str != null ? str.hashCode() : 0);
	}
}
