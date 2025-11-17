/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.retail;

import java.time.Instant;
import java.util.UUID;
import javax.money.MonetaryAmount;

import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.NaturalId;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

/**
 * @author Steve Ebersole
 */
@Entity
public class Product {
	private Integer id;
	private UUID sku;

	private Vendor vendor;

	private MonetaryAmount currentSellPrice;

	@Access( AccessType.FIELD )
	@Version
	@CurrentTimestamp
	private Instant version;

	public Product() {
	}

	public Product(Integer id, UUID sku, Vendor vendor) {
		this.id = id;
		this.sku = sku;
		this.vendor = vendor;
	}

	public Product(Integer id, UUID sku, Vendor vendor, MonetaryAmount currentSellPrice) {
		this.id = id;
		this.sku = sku;
		this.vendor = vendor;
		this.currentSellPrice = currentSellPrice;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumn
	public Vendor getVendor() {
		return vendor;
	}

	public void setVendor(Vendor vendor) {
		this.vendor = vendor;
	}

	@NaturalId
	public UUID getSku() {
		return sku;
	}

	public void setSku(UUID sku) {
		this.sku = sku;
	}

	public MonetaryAmount getCurrentSellPrice() {
		return currentSellPrice;
	}

	public void setCurrentSellPrice(MonetaryAmount currentSellPrice) {
		this.currentSellPrice = currentSellPrice;
	}
}
