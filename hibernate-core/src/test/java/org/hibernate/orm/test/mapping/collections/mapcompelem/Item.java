/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.mapcompelem;


/**
 * @author Gavin King
 */
public class Item {

	private String code;
	private Product product;


	Item() {}
	public Item(String code, Product p) {
		this.code = code;
		this.product = p;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

}
