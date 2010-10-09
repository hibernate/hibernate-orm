//$Id: User.java 7635 2005-07-24 23:04:30Z oneovthafew $
package org.hibernate.test.extralazy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Gavin King
 */
public class User {
	private String name;
	private String password;
	private Map session = new HashMap();
	private Set documents = new HashSet();
	User() {}
	public User(String n, String pw) {
		name=n;
		password = pw;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public Map getSession() {
		return session;
	}
	public void setSession(Map session) {
		this.session = session;
	}
	public Set getDocuments() {
		return documents;
	}
	public void setDocuments(Set documents) {
		this.documents = documents;
	}
}
