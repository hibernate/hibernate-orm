/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.original;
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
	private List permissions = new ArrayList();
	private List emailAddresses = new ArrayList();
	private Map sessionData = new HashMap();
	private Set sessionAttributeNames = new HashSet();

	User() {}
	public User(String name) {
		userName = name;
	}
	public List getPermissions() {
		return permissions;
	}
	public void setPermissions(List permissions) {
		this.permissions = permissions;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public List getEmailAddresses() {
		return emailAddresses;
	}
	public void setEmailAddresses(List emailAddresses) {
		this.emailAddresses = emailAddresses;
	}
	public Map getSessionData() {
		return sessionData;
	}
	public void setSessionData(Map sessionData) {
		this.sessionData = sessionData;
	}
	public Set getSessionAttributeNames() {
		return sessionAttributeNames;
	}
	public void setSessionAttributeNames(Set sessionAttributeNames) {
		this.sessionAttributeNames = sessionAttributeNames;
	}
}
