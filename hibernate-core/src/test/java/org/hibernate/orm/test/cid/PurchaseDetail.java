/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cid;

public class PurchaseDetail {

	private PurchaseRecord purchaseRecord;

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
