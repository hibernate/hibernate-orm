/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.inheritance.cache;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class ExtendedEntity extends MyEntity {
	protected ExtendedEntity() {
	}

	public ExtendedEntity(Integer id, String uid, String extendedValue) {
		super( id, uid );
		this.extendedValue = extendedValue;
	}

	private String extendedValue;

	@Column
	public String getExtendedValue() {
		return extendedValue;
	}

	public void setExtendedValue(final String extendedValue) {
		this.extendedValue = extendedValue;
	}
}
