/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.notinsertable.manytoone;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.envers.Audited;

@Entity
@Audited
public class NotInsertableEntityType {
	public NotInsertableEntityType(Integer typeId, String type) {
		this.typeId = typeId;
		this.type = type;
	}

	public NotInsertableEntityType() {
	}

	@Id
	private Integer typeId;

	@Basic
	private String type;

	public Integer getTypeId() {
		return typeId;
	}

	public void setTypeId(Integer typeId) {
		this.typeId = typeId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof NotInsertableEntityType that) ) {
			return false;
		}

		if ( type != null ? !type.equals( that.getType() ) : that.getType() != null ) {
			return false;
		}
		if ( typeId != null ? !typeId.equals( that.getTypeId() ) : that.getTypeId() != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = typeId != null ? typeId.hashCode() : 0;
		result = 31 * result + (type != null ? type.hashCode() : 0);
		return result;
	}
}
