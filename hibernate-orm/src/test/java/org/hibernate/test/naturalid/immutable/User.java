/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.immutable;


/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class User implements java.io.Serializable {

	private Integer myUserId;
	private Integer version;
	private String userName;
	private String password;
	private String email;

	public User() {
	}

	public User(String userName, String password) {
		this.userName = userName;
		this.password = password;
	}

	public Integer getMyUserId() {
		return this.myUserId;
	}

	public void setMyUserId(Integer myUserId) {
		this.myUserId = myUserId;
	}

	public String getUserName() {
		return this.userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Integer getVersion() {
		return this.version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

}
