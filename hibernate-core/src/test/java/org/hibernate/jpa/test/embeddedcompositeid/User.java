package org.hibernate.jpa.test.embeddedcompositeid;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * @author Habib Zerai
 */
@Access(value = AccessType.FIELD)
@Table(name = "user_")
@Entity(name = "user")
public class User implements Serializable {

	private static final long serialVersionUID = 1L;

	@EmbeddedId
	private UserId codeObject = new UserId();

	@OneToMany(targetEntity = UserRole.class, cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "userProperty", orphanRemoval = true)
	private Set<UserRole> userRoles = new HashSet<>();

	public void setIdentifier(String identifier) {
		this.codeObject.setIdentifier(identifier);
	}

	public UserId getCodeObject() {
		return codeObject;
	}

	public void setVersion(Integer version) {
		this.codeObject.setVersion(version);
	}

	public void addUserRole(UserRole userRole) {
		this.userRoles.add(userRole);
	}

}
