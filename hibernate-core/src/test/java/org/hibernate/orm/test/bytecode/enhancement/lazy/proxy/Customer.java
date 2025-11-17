/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Customer")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Customer {
	private Integer oid;
	private String name;
	private Set<Order> orders = new HashSet<>();

	private Address address;

	private Customer parentCustomer;
	private Set<Customer> childCustomers = new HashSet<>();

	public Customer() {
	}

	public Customer(Integer oid, String name, Address address, Customer parentCustomer) {
		this.oid = oid;
		this.name = name;
		this.address = address;
		this.parentCustomer = parentCustomer;
	}

	@Id
	@Column(name = "oid")
	public Integer getOid() {
		return oid;
	}

	public void setOid(Integer oid) {
		this.oid = oid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "customer")
	public Set<Order> getOrders() {
		return orders;
	}

	public void setOrders(Set<Order> orders) {
		this.orders = orders;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn
	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn
	public Customer getParentCustomer() {
		return parentCustomer;
	}

	public void setParentCustomer(Customer parentCustomer) {
		this.parentCustomer = parentCustomer;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "parentCustomer")
	public Set<Customer> getChildCustomers() {
		return childCustomers;
	}

	public void setChildCustomers(Set<Customer> childCustomers) {
		this.childCustomers = childCustomers;
	}
}
