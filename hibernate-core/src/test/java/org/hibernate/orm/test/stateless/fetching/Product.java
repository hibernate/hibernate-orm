/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless.fetching;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Product {
	@Id
	private Integer id;
	private String sku;

	@ManyToOne( fetch = FetchType.LAZY )
	private Vendor vendor;

	@ManyToOne( fetch = FetchType.LAZY )
	private Producer producer;

	public Product() {
	}

	public Product(Integer id, String sku, Vendor vendor, Producer producer) {
		this.id = id;
		this.sku = sku;
		this.vendor = vendor;
		this.producer = producer;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public Vendor getVendor() {
		return vendor;
	}

	public void setVendor(Vendor vendor) {
		this.vendor = vendor;
	}

	public Producer getProducer() {
		return producer;
	}

	public void setProducer(Producer producer) {
		this.producer = producer;
	}
}
