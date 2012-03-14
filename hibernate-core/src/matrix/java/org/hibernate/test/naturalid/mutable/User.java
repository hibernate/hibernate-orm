//$Id: User.java 6900 2005-05-25 01:24:22Z oneovthafew $
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

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOrg() {
		return org;
	}

	public void setOrg(String org) {
		this.org = org;
	}

	public void setPassword(String password) {
		this.password = password;
	}


}
