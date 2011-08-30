//$Id: Address.java 4390 2004-08-20 07:54:48Z oneovthafew $
package org.hibernate.test.propertyref.basic;


/**
 * @author gavin
 */
public class Address {
	private Long id;
	private String address;
	private String zip;
	private String country;
	private Person person;
	/**
	 * @return Returns the id.
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * @return Returns the person.
	 */
	public Person getPerson() {
		return person;
	}
	/**
	 * @param person The person to set.
	 */
	public void setPerson(Person person) {
		this.person = person;
	}
	/**
	 * @return Returns the address.
	 */
	public String getAddress() {
		return address;
	}
	/**
	 * @param address The address to set.
	 */
	public void setAddress(String address) {
		this.address = address;
	}
	/**
	 * @return Returns the country.
	 */
	public String getCountry() {
		return country;
	}
	/**
	 * @param country The country to set.
	 */
	public void setCountry(String country) {
		this.country = country;
	}
	/**
	 * @return Returns the zip.
	 */
	public String getZip() {
		return zip;
	}
	/**
	 * @param zip The zip to set.
	 */
	public void setZip(String zip) {
		this.zip = zip;
	}
}
