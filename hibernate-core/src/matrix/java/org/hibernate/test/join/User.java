//$Id$
package org.hibernate.test.join;


/**
 * @author Mike Dillon
 */
public class User extends Person {
	private String login;
	private String silly;
	private Double passwordExpiryDays;

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
	/**
	 * @return The password expiry policy in days.
	 */
	public Double getPasswordExpiryDays() {
		return passwordExpiryDays;
	}
	/**
	 * @param passwordExpiryDays The password expiry policy in days. 
	 */
	public void setPasswordExpiryDays(Double passwordExpiryDays) {
		this.passwordExpiryDays = passwordExpiryDays;
	}	
}
