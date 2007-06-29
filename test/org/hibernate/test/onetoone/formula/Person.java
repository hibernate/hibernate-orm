//$Id: Person.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.onetoone.formula;

import java.io.Serializable;

/**
 * @author Gavin King
 */
public class Person implements Serializable {
	private String name;
	private Address address;
	private Address mailingAddress;
	
	public Address getAddress() {
		return address;
	}
	public void setAddress(Address address) {
		this.address = address;
	}
	public Address getMailingAddress() {
		return mailingAddress;
	}
	public void setMailingAddress(Address mailingAddress) {
		this.mailingAddress = mailingAddress;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public boolean equals(Object that) {
		if ( !(that instanceof Person) ) return false;
		Person person = (Person) that;
		return person.getName().equals(name);
	}
	
	public int hashCode() {
		return name.hashCode();
	}
}
