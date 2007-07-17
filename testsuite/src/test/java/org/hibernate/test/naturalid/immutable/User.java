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
	private String firstName;
	private Character initial;
	private String lastName;

	public User() {
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

	public String getFirstName() {
		return this.firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public Character getInitial() {
		return this.initial;
	}

	public void setInitial(Character initial) {
		this.initial = initial;
	}

	public String getLastName() {
		return this.lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Integer getVersion() {
		return this.version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

}
