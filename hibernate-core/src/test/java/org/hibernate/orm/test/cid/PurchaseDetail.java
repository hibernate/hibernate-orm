/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cid;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;

@Entity
public class PurchaseDetail {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumns({
			@JoinColumn(name = "purchaseNumber", nullable = false),
			@JoinColumn(name = "purchaseSequence", nullable = false)
	})
	private PurchaseRecord purchaseRecord;

	@Id
	private String productId;
	private int quantity;

	public PurchaseDetail(PurchaseRecord record, String productId, int quantity) {
		this.productId = productId;
		this.quantity = quantity;
		this.purchaseRecord = record;
	}

	public PurchaseDetail() {}

	public PurchaseRecord getPurchaseRecord() {
		return purchaseRecord;
	}

	public void setPurchaseRecord(PurchaseRecord purchaseRecord) {
		this.purchaseRecord = purchaseRecord;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}
}
