/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.properties;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.envers.Audited;

/**
 * @author Nicolas Doroskevich
 */
@Audited
@Table(name = "UnverOptimLockField")
@Entity
public class UnversionedOptimisticLockingFieldEntity {

	@Id
	@GeneratedValue
	private Integer id;

	private String str;

	@Version
	private int optLocking;

	public UnversionedOptimisticLockingFieldEntity() {
	}

	public UnversionedOptimisticLockingFieldEntity(String str) {
		this.str = str;
	}

	public UnversionedOptimisticLockingFieldEntity(Integer id, String str) {
		this.id = id;
		this.str = str;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public int getOptLocking() {
		return optLocking;
	}

	public void setOptLocking(int optLocking) {
		this.optLocking = optLocking;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof UnversionedOptimisticLockingFieldEntity) ) {
			return false;
		}

		UnversionedOptimisticLockingFieldEntity that = (UnversionedOptimisticLockingFieldEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( str != null ? !str.equals( that.str ) : that.str != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (str != null ? str.hashCode() : 0);
		return result;
	}

}
