/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.custom;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "CUST_TYPE_CONV_ENTITY")
public class MyEntity {
	private Integer id;
	private PayloadWrapper customType;

	public MyEntity() {
	}

	public MyEntity(Integer id, PayloadWrapper customType) {
		this.id = id;
		this.customType = customType;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	// NOTE : this AttributeConverter should be auto-applied here

	@Basic
	public PayloadWrapper getCustomType() {
		return customType;
	}

	public void setCustomType(PayloadWrapper customType) {
		this.customType = customType;
	}
}
