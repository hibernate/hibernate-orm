/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

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
	@LazyToOne(LazyToOneOption.NO_PROXY)
	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn
	@LazyToOne(LazyToOneOption.NO_PROXY)
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
