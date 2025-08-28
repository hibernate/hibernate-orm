/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.superclass.auditoverride;

import jakarta.persistence.Entity;

import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.AuditOverrides;
import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
@AuditOverrides({
						@AuditOverride(forClass = BaseEntity.class, name = "str1", isAudited = true),
						@AuditOverride(forClass = ExtendedBaseEntity.class, name = "number2", isAudited = true)
				})
public class TransitiveOverrideEntity extends ExtendedBaseEntity {
	private String str3;

	public TransitiveOverrideEntity() {
	}

	public TransitiveOverrideEntity(
			String str1,
			Integer number1,
			Integer id,
			String str2,
			Integer number2,
			String str3) {
		super( str1, number1, id, str2, number2 );
		this.str3 = str3;
	}

	public TransitiveOverrideEntity(String str1, Integer number1, String str2, Integer number2, String str3) {
		super( str1, number1, str2, number2 );
		this.str3 = str3;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof TransitiveOverrideEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		TransitiveOverrideEntity that = (TransitiveOverrideEntity) o;

		if ( str3 != null ? !str3.equals( that.str3 ) : that.str3 != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (str3 != null ? str3.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "TransitiveOverrideEntity(" + super.toString() + ", str3 = " + str3 + ")";
	}

	public String getStr3() {
		return str3;
	}

	public void setStr3(String str3) {
		this.str3 = str3;
	}
}
