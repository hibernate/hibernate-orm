/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Person.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
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
