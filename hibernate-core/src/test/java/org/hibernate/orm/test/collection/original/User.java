/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.original;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Gavin King
 */
public class User {
	private String userName;
	private List<Permission> permissions = new ArrayList<>();
	private List<Email> emailAddresses = new ArrayList<>();
	private Map<String, Serializable> sessionData = new HashMap<>();
	private Set<String> sessionAttributeNames = new HashSet<>();

	User() {}

	public User(String name) {
		userName = name;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public List<Permission> getPermissions() {
		return permissions;
	}

	public void setPermissions(List<Permission> permissions) {
		this.permissions = permissions;
	}

	public List<Email> getEmailAddresses() {
		return emailAddresses;
	}

	public void setEmailAddresses(List<Email> emailAddresses) {
		this.emailAddresses = emailAddresses;
	}

	public Map<String, Serializable> getSessionData() {
		return sessionData;
	}

	public void setSessionData(Map<String, Serializable> sessionData) {
		this.sessionData = sessionData;
	}

	public Set<String> getSessionAttributeNames() {
		return sessionAttributeNames;
	}

	public void setSessionAttributeNames(Set<String> sessionAttributeNames) {
		this.sessionAttributeNames = sessionAttributeNames;
	}
}
