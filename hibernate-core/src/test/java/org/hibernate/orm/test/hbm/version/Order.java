/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hbm.version;

/**
 * @author Steve Ebersole
 */
public class Order {
	private Integer id;
	private String referenceCode;
	private byte[] rv;

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getReferenceCode() {
		return this.referenceCode;
	}

	public void setReferenceCode(String referenceCode) {
		this.referenceCode = referenceCode;
	}

	public byte[] getRv() {
		return this.rv;
	}

	public void setRv(byte[] rv) {
		this.rv = rv;
	}
}
