/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.domains.retail;

import javax.money.MonetaryAmount;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Steve Ebersole
 */
@Entity
public class Product {
	private Integer id;
	private Vendor vendor;
	private String sku;
	private MonetaryAmount currentSellPrice;

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

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public MonetaryAmount getCurrentSellPrice() {
		return currentSellPrice;
	}

	public void setCurrentSellPrice(MonetaryAmount currentSellPrice) {
		this.currentSellPrice = currentSellPrice;
	}
}
