/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.ondemandload;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import java.math.BigDecimal;

import org.hibernate.annotations.GenericGenerator;

@Entity
public class Inventory {
	private int id = -1;
	private Store store;
	private Product product;
	private Long quantity;
	private BigDecimal storePrice;

	public Inventory() {
	}

	public Inventory(Store store, Product product) {
		this.store = store;
		this.product = product;
	}

	@Id
	@GeneratedValue
	@GenericGenerator( name = "increment", strategy = "increment" )
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@ManyToOne
	@JoinColumn( name = "store_id" )
	public Store getStore() {
		return store;
	}

	public Inventory setStore(Store store) {
		this.store = store;
		return this;
	}

	@ManyToOne
	@JoinColumn( name = "prod_id" )
	public Product getProduct() {
		return product;
	}

	public Inventory setProduct(Product product) {
		this.product = product;
		return this;
	}

	public Long getQuantity() {
		return quantity;
	}

	public Inventory setQuantity(Long quantity) {
		this.quantity = quantity;
		return this;
	}

	public BigDecimal getStorePrice() {
		return storePrice;
	}

	public Inventory setStorePrice(BigDecimal storePrice) {
		this.storePrice = storePrice;
		return this;
	}
}
