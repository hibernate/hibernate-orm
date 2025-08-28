/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;
import java.util.Collection;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name = "CUSTOMER_TABLE")
public class Customer implements java.io.Serializable {
	private String id;
	private String name;
	private Integer age;
	private Address home;
	private Address work;
	private Country country;
	private Spouse spouse;
	private Collection<CreditCard> creditCards = new java.util.ArrayList<CreditCard>();
	private Collection<Order> orders = new java.util.ArrayList<Order>();
	private Collection<Alias> aliases = new java.util.ArrayList<Alias>();
	private Collection<Alias> aliasesNoop = new java.util.ArrayList<Alias>();

	public Customer() {
	}

	public Customer(String id, String name) {
		this.id = id;
		this.name = name;
	}

	// Used by test case for HHH-8699.
	public Customer(String id, String name, String greeting, Boolean something) {
		this.id = id;
		this.name = name;
	}

	public Customer(String id, String name, Country country) {
		this.id = id;
		this.name = name;
		this.country = country;
	}

	public Customer(String id, String name, Address home,
					Address work, Country country) {
		this.id = id;
		this.name = name;
		this.home = home;
		this.work = work;
		this.country = country;
	}

	@Id
	@Column(name = "ID")
	public String getId() {
		return id;
	}

	public void setId(String v) {
		this.id = v;
	}

	@Column(name = "NAME")
	public String getName() {
		return name;
	}

	public void setName(String v) {
		this.name = v;
	}

	@Column(name = "AGE")
	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	@Embedded
	public Country getCountry() {
		return country;
	}

	public void setCountry(Country v) {
		this.country = v;
	}

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "FK6_FOR_CUSTOMER_TABLE")
	public Address getHome() {
		return home;
	}

	public void setHome(Address v) {
		this.home = v;
	}

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "FK5_FOR_CUSTOMER_TABLE")
	public Address getWork() {
		return work;
	}

	public void setWork(Address v) {
		this.work = v;
	}

	@OneToOne(cascade = CascadeType.ALL, mappedBy = "customer")
	public Spouse getSpouse() {
		return spouse;
	}

	public void setSpouse(Spouse v) {
		this.spouse = v;
	}

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "customer")
	public Collection<CreditCard> getCreditCards() {
		return creditCards;
	}

	public void setCreditCards(Collection<CreditCard> v) {
		this.creditCards = v;
	}

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "customer")
	public Collection<Order> getOrders() {
		return orders;
	}

	public void setOrders(Collection<Order> v) {
		this.orders = v;
	}

	@ManyToMany(cascade = CascadeType.ALL, mappedBy = "customers")
	public Collection<Alias> getAliases() {
		return aliases;
	}

	public void setAliases(Collection<Alias> v) {
		this.aliases = v;
	}

	@ManyToMany(cascade = CascadeType.ALL, mappedBy = "customersNoop")
	public Collection<Alias> getAliasesNoop() {
		return aliasesNoop;
	}

	public void setAliasesNoop(Collection<Alias> v) {
		this.aliasesNoop = v;
	}
}
