/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ondemandload;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Store implements Serializable {
	private int id;
	private String name;
	private List<Inventory> inventories = new ArrayList<Inventory>();
	private int version;

	protected Store() {
	}

	public Store(int id) {
		this.id = id;
	}

	@Id
	@Column(name = "ID")
	public Integer getId() {
		return id;
	}

	private void setId(int id) {
		this.id = id;
	}

	@Column(name = "NAME")
	public String getName() {
		return name;
	}

	public Store setName(String name) {
		this.name = name;
		return this;
	}

	@OneToMany(mappedBy = "store", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	public List<Inventory> getInventories() {
		return inventories;
	}

	public void setInventories(List<Inventory> inventories) {
		this.inventories = inventories;
	}

	public Inventory addInventoryProduct(Product product) {
		final Inventory inventory = new Inventory( this, product );
		this.inventories.add( inventory );
		return inventory;
	}

	@Version
	public int getVersion() {
		return version;
	}

	private void setVersion(int version) {
		this.version = version;
	}
}
