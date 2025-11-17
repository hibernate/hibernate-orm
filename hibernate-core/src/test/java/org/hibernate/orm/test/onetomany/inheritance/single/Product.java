/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetomany.inheritance.single;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

@Entity
@Table(name="PRODTABSG")
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public abstract class Product {

	@Id
	@GeneratedValue
	private int entid;

	@Column(name="INVCODE")
	private String inventoryCode;

	public Product() {

	}

	public Product(String inventoryCode) {
		this.inventoryCode = inventoryCode;
	}

	public int getEntid() {
		return entid;
	}

	public String getInventoryCode() {
		return inventoryCode;
	}
}
