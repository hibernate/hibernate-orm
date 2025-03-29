/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoonewithformula;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "MANUFACTURER")
public class Manufacturer {

	private ManufacturerId id;

	private String name;

	public Manufacturer(ManufacturerId id, String name) {
		this.id = id;
		this.name = name;
	}

	public Manufacturer() {
	}

	@Column(name = "MFG_NAME")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Id
	public ManufacturerId getId() {
		return id;
	}

	public void setId(ManufacturerId id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return name;
	}
}
