/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cid;



/**
 * @author Jacob Robertson
 */
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
	
	
	/**
	 * @return the purchaseRecord
	 */
	public PurchaseRecord getPurchaseRecord() {
		return purchaseRecord;
	}
	/**
	 * @param purchaseRecord the purchaseRecord to set
	 */
	public void setPurchaseRecord(PurchaseRecord purchaseRecord) {
		this.purchaseRecord = purchaseRecord;
	}
	/**
	 * @return the quantity
	 */
	public int getQuantity() {
		return quantity;
	}
	/**
	 * @param quantity the quantity to set
	 */
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	/**
	 * @return the productId
	 */
	public String getProductId() {
		return productId;
	}
	/**
	 * @param productId the productId to set
	 */
	public void setProductId(String productId) {
		this.productId = productId;
	}
}
