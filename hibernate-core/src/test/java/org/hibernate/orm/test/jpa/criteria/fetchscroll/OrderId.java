/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.fetchscroll;

import java.io.Serializable;
import jakarta.persistence.*;

@Embeddable
@MappedSuperclass
public class OrderId implements Serializable {

	private Long purchaseOrgId;
	private String number;

	@Column(name = "PURCHASE_ORG_ID", nullable = false, updatable = false)
	public Long getPurchaseOrgId() {
		return purchaseOrgId;
	}

	public void setPurchaseOrgId(Long purchaseOrgId) {
		this.purchaseOrgId = purchaseOrgId;
	}

	@Column(name = "ORDER_NUMBER", length = 40, nullable = false, updatable = false)
	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

}
