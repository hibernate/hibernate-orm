//$Id: Address.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.typedonetoone;
import java.io.Serializable;

/**
 * @author Gavin King
 */
public class Address implements Serializable {
	
	private AddressId addressId;
	private String street;
	private String city;
	private String state;
	private String zip;
	private Customer customer;

	public Customer getCustomer() {
		return customer;
	}
	public void setCustomer(Customer customer) {
		this.customer = customer;
	}
	
	public String getCity() {
		return city;
	}
	public void setCity(String city) {
		this.city = city;
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
	public AddressId getAddressId() {
		return addressId;
	}
	public void setAddressId(AddressId addressId) {
		this.addressId = addressId;
	}
}
