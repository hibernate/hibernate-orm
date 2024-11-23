package org.hibernate.test.annotations.fetchprofile;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.test.annotations.fetchprofile.mappedby.Address;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
@FetchProfile(name = "customer-with-address", fetchOverrides = {
		@FetchProfile.FetchOverride(entity = Customer6.class, association = "address", mode = FetchMode.JOIN)
})
public class Customer6 {

	@Id
	@GeneratedValue
	private long id;

	private String name;

	@OneToOne(fetch = FetchType.LAZY)
	private Address address;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

}
