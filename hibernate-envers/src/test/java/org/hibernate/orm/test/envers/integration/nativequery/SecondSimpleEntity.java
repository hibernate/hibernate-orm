/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.nativequery;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Andrea Boriero
 */
@Audited
@Entity
public class SecondSimpleEntity {
	@Id
	@GeneratedValue
	private Long id;

	private String stringField;

	public SecondSimpleEntity() {
	}

	public SecondSimpleEntity(String stringField) {
		this.stringField = stringField;
	}

	public Long getId() {
		return id;
	}

	public String getStringField() {
		return stringField;
	}

	public void setStringField(String stringField) {
		this.stringField = stringField;
	}
}
