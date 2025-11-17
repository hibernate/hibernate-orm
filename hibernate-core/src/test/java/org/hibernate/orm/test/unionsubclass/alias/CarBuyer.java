/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unionsubclass.alias;


/**
 *
 * @author Strong Liu
 */
public class CarBuyer extends Customer {
	private String sellerName;
	private String pid;
	private Seller seller;

	public String getSellerName() {
		return sellerName;
	}

	public void setSellerName( String sellerName ) {
		this.sellerName = sellerName;
	}

	public String getPid() {
		return pid;
	}

	public void setPid( String pid ) {
		this.pid = pid;
	}

	public Seller getSeller() {
		return seller;
	}

	public void setSeller( Seller seller ) {
		this.seller = seller;
	}

}
