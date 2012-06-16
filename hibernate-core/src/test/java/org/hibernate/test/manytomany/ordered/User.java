package org.hibernate.test.manytomany.ordered;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class User implements Serializable {

	private Long id;
	private String org;
	private String name;
	private Set groups = new HashSet();

	public User() {
	}

	public User(String name, String org) {
		this.org = org;
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public Set getGroups() {
		return groups;
	}

	public void setGroups(Set groups) {
		this.groups = groups;
	}

}
