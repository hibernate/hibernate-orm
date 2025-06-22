/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.auditReader;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class NotAuditedTestEntity {
	@Id
	private Integer id;

	private String str1;

	public NotAuditedTestEntity() {
	}

	public NotAuditedTestEntity(String str1) {
		this.str1 = str1;
	}

	public NotAuditedTestEntity(Integer id, String str1) {
		this.id = id;
		this.str1 = str1;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getStr1() {
		return str1;
	}

	public void setStr1(String str1) {
		this.str1 = str1;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof NotAuditedTestEntity) ) {
			return false;
		}

		NotAuditedTestEntity that = (NotAuditedTestEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( str1 != null ? !str1.equals( that.str1 ) : that.str1 != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (str1 != null ? str1.hashCode() : 0);
		return result;
	}
}
