package org.hibernate.test.orphan.manytomany;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class User implements Serializable {

	private String org;

	private String name;

	private Map<Integer, Group> groups = new HashMap<Integer, Group>();

	public User(String name, String org) {
		this.org = org;
		this.name = name;
	}

	public User() {
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

	public Map<Integer, Group> getGroups() {
		return groups;
	}

	public void setGroups(Map<Integer, Group> groups) {
		this.groups = groups;
	}

}
