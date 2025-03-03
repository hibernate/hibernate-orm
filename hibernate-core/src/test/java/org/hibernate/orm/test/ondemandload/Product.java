/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ondemandload;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
public class Product implements Serializable {
	private String id;
	private String name;
	private String description;
	private BigDecimal msrp;
	private int version;

	private Product() {
	}

	public Product(String id) {
		this.id = id;
	}

	@Id
	public String getId() {
		return id;
	}

	private void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public Product setName(String name) {
		this.name = name;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public Product setDescription(String description) {
		this.description = description;
		return this;
	}

	public BigDecimal getMsrp() {
		return msrp;
	}

	public Product setMsrp(BigDecimal msrp) {
		this.msrp = msrp;
		return this;
	}

	@Version
	public int getVersion() {
		return version;
	}

	private void setVersion(int version) {
		this.version = version;
	}
}
