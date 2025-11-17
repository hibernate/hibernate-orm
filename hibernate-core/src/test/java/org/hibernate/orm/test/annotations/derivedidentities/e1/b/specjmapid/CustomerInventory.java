/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e1.b.specjmapid;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Version;

@NamedQueries({
		@NamedQuery(name = "CustomerInventory.selectAll",
				query = "select a from CustomerInventory a")
})
@Entity
@Table(name = "O_CUSTINVENTORY")
@IdClass(CustomerInventoryPK.class)
public class CustomerInventory implements Serializable, Comparator<CustomerInventory> {

	@Id
	@TableGenerator(name = "inventory",
			table = "U_SEQUENCES",
			pkColumnName = "S_ID",
			valueColumnName = "S_NEXTNUM",
			pkColumnValue = "inventory",
			allocationSize = 1000)
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "inventory")
	@Column(name = "CI_ID")
	private Integer id;

	@Id
	@Column(name = "CI_CUSTOMERID", insertable = false, updatable = false)
	private int custId;

	@ManyToOne(cascade = CascadeType.MERGE)
	@JoinColumn(name = "CI_CUSTOMERID", nullable = false)
	@MapsId("custId")
	private Customer customer;

	@ManyToOne(cascade = CascadeType.MERGE)
	@JoinColumn(name = "CI_ITEMID")
	private Item vehicle;

	@Column(name = "CI_VALUE")
	private BigDecimal totalCost;

	@Column(name = "CI_QUANTITY")
	private int quantity;

	@Version
	@Column(name = "CI_VERSION")
	private int version;

	protected CustomerInventory() {
	}

	CustomerInventory(Customer customer, Item vehicle, int quantity, BigDecimal totalValue) {
		this.customer = customer;
		this.vehicle = vehicle;
		this.quantity = quantity;
		this.totalCost = totalValue;
	}

	public Item getVehicle() {
		return vehicle;
	}

	public BigDecimal getTotalCost() {
		return totalCost;
	}

	public int getQuantity() {
		return quantity;
	}

	public Integer getId() {
		return id;
	}

	public Customer getCustomer() {
		return customer;
	}

	public int getCustId() {
		return custId;
	}

	public int getVersion() {
		return version;
	}

	public int compare(CustomerInventory cdb1, CustomerInventory cdb2) {
		return cdb1.id.compareTo( cdb2.id );
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == this ) {
			return true;
		}
		if ( !(obj instanceof CustomerInventory inventory) ) {
			return false;
		}
		if ( this.id != null && inventory.id == null ) {
			return false;
		}
		if ( this.id == null && inventory.id != null ) {
			return false;
		}
		return Objects.equals( this.id, inventory.id );
	}

}
