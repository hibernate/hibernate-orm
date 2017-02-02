/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: User.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.collection.original;
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
