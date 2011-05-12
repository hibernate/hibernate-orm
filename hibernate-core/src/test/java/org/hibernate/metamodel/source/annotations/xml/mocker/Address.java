package org.hibernate.metamodel.source.annotations.xml.mocker;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author Strong Liu
 */
@Embeddable
public class Address {
	private String country;
	private String city;
	private String zip;

	@Column(name = "ADD_CITY")
	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	@Column(name = "ADD_COUNTRY")
	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	@Column(name = "ADD_ZIP")
	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}
}
