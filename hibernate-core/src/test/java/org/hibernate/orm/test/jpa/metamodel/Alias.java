/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;
import java.util.Collection;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name = "ALIAS_TABLE")
public class Alias implements java.io.Serializable {
	private String id;
	private String alias;
	private Customer customerNoop;
	private Collection<Customer> customersNoop = new java.util.ArrayList<Customer>();
	private Collection<Customer> customers = new java.util.ArrayList<Customer>();

	public Alias() {
	}

	public Alias(String id, String alias) {
		this.id = id;
		this.alias = alias;
	}

	@Id
	@Column(name = "ID")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Column(name = "ALIAS")
	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "FK1_FOR_CUSTOMER_TABLE", insertable = false, updatable = false)
	public Customer getCustomerNoop() {
		return customerNoop;
	}

	public void setCustomerNoop(Customer customerNoop) {
		this.customerNoop = customerNoop;
	}

	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name = "FKS_ANOOP_CNOOP",
			joinColumns =
			@JoinColumn(
					name = "FK2_FOR_ALIAS_TABLE", referencedColumnName = "ID"),
			inverseJoinColumns =
			@JoinColumn(
					name = "FK8_FOR_CUSTOMER_TABLE", referencedColumnName = "ID")
	)
	public Collection<Customer> getCustomersNoop() {
		return customersNoop;
	}

	public void setCustomersNoop(Collection<Customer> customersNoop) {
		this.customersNoop = customersNoop;

	}

	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name = "FKS_ALIAS_CUSTOMER",
			joinColumns =
			@JoinColumn(
					name = "FK_FOR_ALIAS_TABLE", referencedColumnName = "ID"),
			inverseJoinColumns =
			@JoinColumn(
					name = "FK_FOR_CUSTOMER_TABLE", referencedColumnName = "ID")
	)
	public Collection<Customer> getCustomers() {
		return customers;
	}

	public void setCustomers(Collection<Customer> customers) {
		this.customers = customers;
	}

}
