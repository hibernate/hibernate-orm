/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.tableperclass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity(name = "xpmComponent")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(indexes = @Index(name = "manufacturerPartNumber", columnList = "manufacturerPartNumber"))
public abstract class Component {
	private String manufacturerPartNumber;
	private Long manufacturerId;
	private Long id;

	public void setId(Long id) {
		this.id = id;
	}

	@Id
	public Long getId() {
		return id;
	}

	@Column(nullable = false)
	public String getManufacturerPartNumber() {
		return manufacturerPartNumber;
	}

	@Column(nullable = false)
	public Long getManufacturerId() {
		return manufacturerId;
	}

	public void setManufacturerId(Long manufacturerId) {
		this.manufacturerId = manufacturerId;
	}


	public void setManufacturerPartNumber(String manufacturerPartNumber) {
		this.manufacturerPartNumber = manufacturerPartNumber;
	}
}
