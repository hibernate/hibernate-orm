//$Id$
package org.hibernate.test.usercollection.basic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gavin King
 */
public class User {
	private String userName;
	private IMyList emailAddresses = new MyList();
	private Map sessionData = new HashMap();

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
	public List getEmailAddresses() {
		return emailAddresses;
	}
	public void setEmailAddresses(IMyList emailAddresses) {
		this.emailAddresses = emailAddresses;
	}
	public Map getSessionData() {
		return sessionData;
	}
	public void setSessionData(Map sessionData) {
		this.sessionData = sessionData;
	}
}
