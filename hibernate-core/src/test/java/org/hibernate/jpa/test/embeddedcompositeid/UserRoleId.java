package org.hibernate.jpa.test.embeddedcompositeid;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;

/**
 * @author Habib Zerai
 *
 */
@Embeddable
public class UserRoleId implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Embedded
	private RoleId role;
	
	@Embedded
	private UserId userProperty;

	public RoleId getRole() {
		return role;
	}

	public void setRoleId(RoleId roleId) {
		this.role = roleId;
	}

	public UserId getUser() {
		return userProperty;
	}

	public void setUserId(UserId userId) {
		this.userProperty = userId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(role, userProperty);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserRoleId other = (UserRoleId) obj;
		return Objects.equals(role, other.role) && Objects.equals(userProperty, other.userProperty);
	}

}
