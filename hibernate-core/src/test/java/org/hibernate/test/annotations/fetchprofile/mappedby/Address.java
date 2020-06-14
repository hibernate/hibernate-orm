package org.hibernate.test.annotations.fetchprofile.mappedby;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.test.annotations.fetchprofile.Customer6;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
@FetchProfile(name = "address-with-customer", fetchOverrides = {
		@FetchProfile.FetchOverride(entity = Address.class, association = "customer", mode = FetchMode.JOIN)
})
public class Address {

	@Id
	@GeneratedValue
	private long id;

	private String street;

	@OneToOne(fetch = FetchType.LAZY, mappedBy = "address")
	private Customer6 customer;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public Customer6 getCustomer() {
		return customer;
	}

	public void setCustomer(Customer6 customer) {
		this.customer = customer;
	}

}
