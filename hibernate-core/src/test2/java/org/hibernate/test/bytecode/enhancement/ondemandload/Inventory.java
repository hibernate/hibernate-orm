/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.ondemandload;

import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

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
