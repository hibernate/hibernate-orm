//$Id: Person.java 10396 2006-09-01 08:48:02 -0500 (Fri, 01 Sep 2006) steve.ebersole@jboss.com $
package org.hibernate.test.propertyref.basic;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author gavin
 */
public class Person {
	private Long id;
	private String name;
	private Address address;
	private String userId;
	private Set accounts = new HashSet();
	private List systems = new ArrayList();

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

	public List getSystems() {
		return systems;
	}

	public void setSystems(List systems) {
		this.systems = systems;
	}
}
