/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.inheritance.joined.primarykeyjoin;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;

import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.integration.inheritance.joined.ParentEntity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
@PrimaryKeyJoinColumn(name = "other_id")
public class ChildPrimaryKeyJoinEntity extends ParentEntity {
	@Basic
	private Long namVal;

	public ChildPrimaryKeyJoinEntity() {
	}

	public ChildPrimaryKeyJoinEntity(Integer id, String data, Long namVal) {
		super( id, data );
		this.namVal = namVal;
	}

	public Long getNumVal() {
		return namVal;
	}

	public void setNumVal(Long namVal) {
		this.namVal = namVal;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ChildPrimaryKeyJoinEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		ChildPrimaryKeyJoinEntity childPrimaryKeyJoinEntity = (ChildPrimaryKeyJoinEntity) o;

		//noinspection RedundantIfStatement
		if ( namVal != null ?
				!namVal.equals( childPrimaryKeyJoinEntity.namVal ) :
				childPrimaryKeyJoinEntity.namVal != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (namVal != null ? namVal.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "CPKJE(id = " + getId() + ", data = " + getData() + ", namVal = " + namVal + ")";
	}
}
