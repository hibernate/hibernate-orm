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
public class UserId implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@javax.persistence.Column(name = "identifier_", unique = false, nullable = false, insertable = true, updatable = false, length = 255)
	@Basic(fetch = FetchType.EAGER, optional = false)
	private String identifier = null;
	@javax.persistence.Column(name = "version_", unique = false, nullable = false, insertable = true, updatable = false, precision = 11, scale = 0)
	@Basic(fetch = FetchType.EAGER, optional = false)
	private Integer version = null;

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	@Override
	public int hashCode() {
		return Objects.hash(identifier, version);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserId other = (UserId) obj;
		return Objects.equals(identifier, other.identifier) && Objects.equals(version, other.version);
	}

}
