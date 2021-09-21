/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Person.java 4592 2004-09-26 00:39:43Z oneovthafew $
package org.hibernate.test.cuk;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author gavin
 */
public class Person implements Serializable {
	private Long id;
	private String name;
	private Address address;
	private String userId;
	private boolean deleted;
	private Set accounts = new HashSet();
	/**
	 * @return Returns the userId.
	 */
	public String getUserId() {
		return userId;
	}
	/**
	 * @param userId The userId to set.
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}
	/**
	 * @return Returns the address.
	 */
	public Address getAddress() {
		return address;
	}
	/**
	 * @param address The address to set.
	 */
	public void setAddress(Address address) {
		this.address = address;
	}
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
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return Returns the accounts.
	 */
	public Set getAccounts() {
		return accounts;
	}
	/**
	 * @param accounts The accounts to set.
	 */
	public void setAccounts(Set accounts) {
		this.accounts = accounts;
	}
	
	public boolean isDeleted() {
		return deleted;
	}
	
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
	
	public boolean equals(Object other) {
		if (other instanceof Person) {
			Person that = (Person) other;
			return that.isDeleted() == deleted && that.getUserId().equals(userId);
		}
		else {
			return false;
		}
	}
	
	public int hashCode() {
		return userId.hashCode();
	}
}
