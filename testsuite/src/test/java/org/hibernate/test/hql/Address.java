// $Id: Address.java 7996 2005-08-22 14:49:57Z steveebersole $
package org.hibernate.test.hql;

/**
 * Implementation of Address.
 *
 * @author Steve Ebersole
 */
public class Address {
	private String street;
	private String city;
	private String postalCode;
	private String country;
	private StateProvince stateProvince;

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public StateProvince getStateProvince() {
		return stateProvince;
	}

	public void setStateProvince(StateProvince stateProvince) {
		this.stateProvince = stateProvince;
	}
}
