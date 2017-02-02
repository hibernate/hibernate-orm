/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Account.java 4592 2004-09-26 00:39:43Z oneovthafew $
package org.hibernate.test.cuk;
import java.io.Serializable;

/**
 * @author Gavin King
 */
public class Account implements Serializable {
	private String accountId;
	private Person user;
	private char type;
	/**
	 * @return Returns the user.
	 */
	public Person getUser() {
		return user;
	}
	/**
	 * @param user The user to set.
	 */
	public void setUser(Person user) {
		this.user = user;
	}
	/**
	 * @return Returns the accountId.
	 */
	public String getAccountId() {
		return accountId;
	}
	/**
	 * @param accountId The accountId to set.
	 */
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}
	/**
	 * @return Returns the type.
	 */
	public char getType() {
		return type;
	}
	/**
	 * @param type The type to set.
	 */
	public void setType(char type) {
		this.type = type;
	}

}
