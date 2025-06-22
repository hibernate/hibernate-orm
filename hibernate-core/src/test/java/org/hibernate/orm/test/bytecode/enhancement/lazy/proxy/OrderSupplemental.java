/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "OrderSupplemental")
@Table(name = "order_supp")
public class OrderSupplemental {
	private Integer oid;
	private Integer receivablesId;

	public OrderSupplemental() {
	}

	public OrderSupplemental(Integer oid, Integer receivablesId) {
		this.oid = oid;
		this.receivablesId = receivablesId;
	}

	@Id
	@Column(name = "oid")
	public Integer getOid() {
		return oid;
	}

	public void setOid(Integer oid) {
		this.oid = oid;
	}

	public Integer getReceivablesId() {
		return receivablesId;
	}

	public void setReceivablesId(Integer receivablesId) {
		this.receivablesId = receivablesId;
	}
}
