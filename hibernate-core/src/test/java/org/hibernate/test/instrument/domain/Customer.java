package org.hibernate.test.instrument.domain;


import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

@Entity( name = "Customer" )
@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
public abstract class Customer {
	private Integer oid;
	private String name;
	private Set<Order> orders = new HashSet<Order>();

	private Address address;

	private Customer parentCustomer;
	private Set<Customer> childCustomers = new HashSet<Customer>();

	public Customer() {
	}

	public Customer(Integer oid, String name, Address address, Customer parentCustomer) {
		this.oid = oid;
		this.name = name;
		this.address = address;
		this.parentCustomer = parentCustomer;
	}

	@Id
	@Column( name = "oid" )
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

	@OneToMany( fetch = FetchType.LAZY, mappedBy = "customer" )
	public Set<Order> getOrders() {
		return orders;
	}

	public void setOrders(Set<Order> orders) {
		this.orders = orders;
	}

	@ManyToOne( fetch = FetchType.LAZY )
	@JoinColumn
	@LazyToOne( LazyToOneOption.NO_PROXY )
	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	@ManyToOne( fetch = FetchType.LAZY )
	@JoinColumn
	@LazyToOne( LazyToOneOption.NO_PROXY )
	public Customer getParentCustomer() {
		return parentCustomer;
	}

	public void setParentCustomer(Customer parentCustomer) {
		this.parentCustomer = parentCustomer;
	}

	@OneToMany( fetch = FetchType.LAZY, mappedBy = "parentCustomer" )
	public Set<Customer> getChildCustomers() {
		return childCustomers;
	}

	public void setChildCustomers(Set<Customer> childCustomers) {
		this.childCustomers = childCustomers;
	}
}
