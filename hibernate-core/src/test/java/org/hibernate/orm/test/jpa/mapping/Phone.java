/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.mapping;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "PHONE")
public class Phone implements java.io.Serializable {

	private Integer id;
	private String brandName;
	private float price;

	public Phone() {
	}

	@Id
	@Column(name="ID")
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name="BRANDNAME")
	public String getBrandName() {
		return brandName;
	}
	public void setBrandName(String bName) {
		this.brandName = bName;
	}

	@Column(name="PRICE")
	public float getPrice() {
		return price;
	}
	public void setPrice(float price) {
		this.price = price;
	}
}
