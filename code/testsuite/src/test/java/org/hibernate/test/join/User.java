//$Id$
package org.hibernate.test.join;

/**
 * @author Mike Dillon
 */
public class User extends Person {
	private String login;
	private String silly;

	/**
	 * @return Returns the login.
	 */
	public String getLogin() {
		return login;
	}
	/**
	 * @param login The login to set.
	 */
	public void setLogin(String login) {
		this.login = login;
	}
}
