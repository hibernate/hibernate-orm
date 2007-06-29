//$Id$
package org.hibernate.test.naturalid.mutable;

/**
 * @author Gavin King
 */
public class User {
	
	private Long id;
	private String name;
	private String org;
	private String password;
	
	User() {}

	public User(String name, String org, String password) {
		this.name = name;
		this.org = org;
		this.password = password;
	}

	public String getName() {
		return name;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getOrg() {
		return org;
	}
	
}
