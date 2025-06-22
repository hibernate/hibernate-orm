/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.components.joins;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Matt Todd
 */
@Entity
public class ManyToOneType {
	@Id
	@GeneratedValue
	public Long id;
	@Column(name = "val")
	private String value;

	public ManyToOneType() {
	}

	public ManyToOneType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
