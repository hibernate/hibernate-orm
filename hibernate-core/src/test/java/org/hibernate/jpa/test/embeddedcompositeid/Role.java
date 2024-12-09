package org.hibernate.jpa.test.embeddedcompositeid;

import java.io.Serializable;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * @author Habib Zerai
 */
@Access(value = AccessType.FIELD)
@Entity(name = "role")
@Table(name = "role_")
public class Role implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@EmbeddedId
	private RoleId codeObject = new RoleId();

	@OneToMany(targetEntity = UserRole.class, fetch = FetchType.LAZY, mappedBy = "role")
	private Set<UserRole> userRoles = new java.util.HashSet<>();

	public void setIdentifier(String identifier) {
		this.codeObject.setIdentifier(identifier);
	}

	public RoleId getCodeObject() {
		return codeObject;
	}

}
