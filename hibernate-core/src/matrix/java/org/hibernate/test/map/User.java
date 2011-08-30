//$Id: User.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.map;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Gavin King
 */
public class User {
	private String name;
	private String password;
	private Map session = new HashMap();
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
}
