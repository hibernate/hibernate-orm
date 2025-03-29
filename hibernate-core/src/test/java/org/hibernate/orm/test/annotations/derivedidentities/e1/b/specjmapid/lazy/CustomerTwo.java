/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e1.b.specjmapid.lazy;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;

import org.hibernate.orm.test.annotations.derivedidentities.e1.b.specjmapid.Item;

@NamedQueries({
		@NamedQuery(name = CustomerTwo.QUERY_ALL,
				query = "select a from CustomerTwo a"),
		@NamedQuery(name = CustomerTwo.QUERY_COUNT,
				query = "select COUNT(a) from CustomerTwo a"),
		@NamedQuery(name = CustomerTwo.QUERY_BY_CREDIT,
				query = "SELECT c.id FROM CustomerTwo c WHERE c.creditLimit > :limit")
})
@Entity
@Table(name = "O_CUSTOMER")
public class CustomerTwo implements Serializable{
	public static final String QUERY_ALL = "CustomerTwo.selectAll";
	public static final String QUERY_COUNT = "CustomerTwo.count";
	public static final String QUERY_BY_CREDIT = "CustomerTwo.selectByCreditLimit";

	public static final String BAD_CREDIT = "BC";

	@Id
	@Column(name = "C_ID")
	private int id;

	@Column(name = "C_FIRST")
	private String firstName;

	@Column(name = "C_LAST")
	private String lastName;

	@Column(name = "C_CONTACT")
	private String contact;

	@Column(name = "C_CREDIT")
	private String credit;

	@Column(name = "C_CREDIT_LIMIT")
	private BigDecimal creditLimit;

	@Column(name = "C_SINCE")
	@Temporal(TemporalType.DATE)
	private Calendar since;

	@Column(name = "C_BALANCE")
	private BigDecimal balance;

	@Column(name = "C_YTD_PAYMENT")
	private BigDecimal ytdPayment;

	@OneToMany(targetEntity = CustomerInventoryTwo.class,
			mappedBy = "customer",
			cascade = CascadeType.ALL,
			fetch = FetchType.EAGER)
	private List<CustomerInventoryTwo> customerInventories;


	@Version
	@Column(name = "C_VERSION")
	private int version;

	protected CustomerTwo() {
	}

	public CustomerTwo(String first, String last,
					String contact, String credit, BigDecimal creditLimit,
					BigDecimal balance, BigDecimal YtdPayment) {

		this.firstName = first;
		this.lastName = last;
		this.contact = contact;
		this.since = Calendar.getInstance();
		this.credit = credit;
		this.creditLimit = creditLimit;
		this.balance = balance;
		this.ytdPayment = YtdPayment;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer customerId) {
		this.id = customerId;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getContact() {
		return contact;
	}

	public void setContact(String contact) {
		this.contact = contact;
	}

	public String getCredit() {
		return credit;
	}

	public void setCredit(String credit) {
		this.credit = credit;
	}

	public BigDecimal getCreditLimit() {
		return creditLimit;
	}

	public void setCreditLimit(BigDecimal creditLimit) {
		this.creditLimit = creditLimit;
	}

	public Calendar getSince() {
		return since;
	}

	public void setSince(Calendar since) {
		this.since = since;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public void changeBalance(BigDecimal change) {
		setBalance( balance.add( change ).setScale( 2, BigDecimal.ROUND_DOWN ) );
	}

	public BigDecimal getYtdPayment() {
		return ytdPayment;
	}

	public void setYtdPayment(BigDecimal ytdPayment) {
		this.ytdPayment = ytdPayment;
	}


	public List<CustomerInventoryTwo> getInventories() {
		if ( this.customerInventories == null ) {
			this.customerInventories = new ArrayList<CustomerInventoryTwo>();
		}
		return this.customerInventories;
	}

	public CustomerInventoryTwo addInventory(Item item, int quantity,
										BigDecimal totalValue) {

		CustomerInventoryTwo inventory = new CustomerInventoryTwo(
				this, item,
				quantity, totalValue
		);
		getInventories().add( inventory );
		return inventory;
	}

	public int getVersion() {
		return version;
	}

	public boolean hasSufficientCredit(BigDecimal amount) {
		return !BAD_CREDIT.equals( getCredit() )
				&& creditLimit != null
				&& creditLimit.compareTo( amount ) >= 0;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		return id == ( ( CustomerTwo ) o ).id;
	}

	@Override
	public int hashCode() {
		return new Integer( id ).hashCode();
	}

	@Override
	public String toString() {
		return this.getFirstName() + " " + this.getLastName();
	}
}
