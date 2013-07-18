package org.hibernate.test.annotations.namingstrategy;
import javax.persistence.Embeddable;

@Embeddable
public class AddressEntry implements java.io.Serializable {
	protected String street;
	protected String city;
	protected String state;
	protected String zip;

	public AddressEntry() {
	}

 	public AddressEntry( String street, String city, String state, String zip) {
		this.street = street;
		this.city = city;
		this.state = state;
		this.zip = zip;
	}
	public String getCity() {
		return city;
	}
	public void setCity(String c) {
		city = c;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getStreet() {
		return street;
	}
	public void setStreet(String street) {
		this.street = street;
	}
	public String getZip() {
		return zip;
	}
	public void setZip(String zip) {
		this.zip = zip;
	}
}
