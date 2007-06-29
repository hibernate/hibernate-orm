//$Id: Person.java 7192 2005-06-18 14:40:15Z oneovthafew $
package org.hibernate.test.unionsubclass2;


/**
 * @author Gavin King
 */
public class Person {
	private long id;
	private String name;
	private char sex;
	private Address address = new Address();
	/**
	 * @return Returns the address.
	 */
	public Address getAddress() {
		return address;
	}

	public void setAddress(String string) {
		this.address.address = string;
	}

	public void setZip(String string) {
		this.address.zip = string;
	}

	public void setCountry(String string) {
		this.address.country = string;
	}

	
	/**
	 * @return Returns the sex.
	 */
	public char getSex() {
		return sex;
	}
	/**
	 * @param sex The sex to set.
	 */
	public void setSex(char sex) {
		this.sex = sex;
	}
	/**
	 * @return Returns the id.
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * @return Returns the identity.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param identity The identity to set.
	 */
	public void setName(String identity) {
		this.name = identity;
	}

}
