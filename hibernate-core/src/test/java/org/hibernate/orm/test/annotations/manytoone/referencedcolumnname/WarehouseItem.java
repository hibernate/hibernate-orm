/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoone.referencedcolumnname;
import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class WarehouseItem extends GenericObject {

	Item item;
	Vendor vendor;
	ZItemCost defaultCost;
	BigDecimal qtyInStock;


	public BigDecimal getQtyInStock() {
		return qtyInStock;
	}

	public void setQtyInStock(BigDecimal qtyInStock) {
		this.qtyInStock = qtyInStock;
	}

	@ManyToOne
//(fetch=FetchType.LAZY)
	@JoinColumn( name = "item_id", nullable = false)
	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	@ManyToOne( fetch = FetchType.LAZY )
	@JoinColumn( name = "vendor_id",  nullable = false)
	public Vendor getVendor() {
		return vendor;
	}

	public void setVendor(Vendor vendor) {
		this.vendor = vendor;
	}

	@ManyToOne
	@JoinColumn( name = "vendor_id", referencedColumnName = "vendor_id", insertable = false, updatable = false )
	@JoinColumn( name = "item_id", referencedColumnName = "item_id", insertable = false, updatable = false )
	public ZItemCost getDefaultCost() {
		return defaultCost;
	}

	public void setDefaultCost(ZItemCost defaultCost) {
		this.defaultCost = defaultCost;
	}
}
