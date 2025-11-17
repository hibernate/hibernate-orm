/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.contributor.usertype.hhh18787;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Some entity, important is the property <code>customData</code>.
 */
@Entity
@Table(name = "whatever")
public class SomeEntity {
	@Id
	@GeneratedValue
	private Long id;

	@Column
	private CustomData[] customData;

	public SomeEntity() {
	}

	public SomeEntity(CustomData[] customData) {
		this.customData = customData;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public CustomData[] getCustomData() {
		return customData;
	}

	public void setCustomData(CustomData[] custom) {
		this.customData = custom;
	}
}
