package org.hibernate.jpa.test.embeddedcompositeid;

import java.io.Serializable;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

/**
 * @author Habib Zerai
 */
@Access(value = AccessType.FIELD)
@Entity(name = "userRole")
@Table(name = "userRole_")
public class UserRole implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@EmbeddedId
	private UserRoleId codeObject = new UserRoleId();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "role_identifier_", referencedColumnName = "identifier_", unique = false, nullable = false)
	@MapsId("role")
	private Role role;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumns({
			@JoinColumn(name = "userProperty_identifier_", referencedColumnName = "identifier_", unique = false, nullable = false),
			@JoinColumn(name = "userProperty_version_", referencedColumnName = "version_", unique = false, nullable = false) })
	@MapsId("userProperty")
	private User userProperty;

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.codeObject.setRoleId(role.getCodeObject());
		this.role = role;
	}

	public User getUser() {
		return userProperty;
	}

	public void setUser(User user) {
		this.codeObject.setUserId(user.getCodeObject());
		this.userProperty = user;
	}

	public UserRoleId getCodeObject() {
		return this.codeObject;
	}

}
