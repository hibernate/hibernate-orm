package org.hibernate.jpa.test.embeddedcompositeid;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;

/**
 * @author Habib Zerai
 */
@Embeddable
public class RoleId implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	@javax.persistence.Column(name = "identifier_", unique = false, nullable = false, insertable = true, updatable = false, length = 255)
	@Basic(fetch = FetchType.EAGER, optional = false)
	private String identifier = null;

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	@Override
	public int hashCode() {
		return Objects.hash(identifier);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RoleId other = (RoleId) obj;
		return Objects.equals(identifier, other.identifier);
	}

}
