/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.nativequery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Andrea Boriero
 */
@Entity(name = "SimpleEntity")
@Audited
public class SimpleEntity {
	@Id
	@GeneratedValue
	private Long id;

	@Column(name = "string_field")
	private String stringField;

	public SimpleEntity() {
	}

	public SimpleEntity(String stringField) {
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
