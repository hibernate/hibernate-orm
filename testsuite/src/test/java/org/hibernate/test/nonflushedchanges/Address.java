package org.hibernate.test.nonflushedchanges;

import java.io.Serializable;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole, Gail Badner (adapted this from "ops" tests version)
 */
public class Address implements Serializable {
	private Long id;
	private String streetAddress;
	private String city;
	private String country;
	private Person resident;

	public Address() {
	}

	public Address(String streetAddress, String city, String country, Person resident) {
		this.streetAddress = streetAddress;
		this.city = city;
		this.country = country;
		this.resident = resident;
		resident.setAddress( this );
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getStreetAddress() {
		return streetAddress;
	}

	public void setStreetAddress(String streetAddress) {
		this.streetAddress = streetAddress;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public Person getResident() {
		return resident;
	}

	public void setResident(Person resident) {
		this.resident = resident;
	}
}
