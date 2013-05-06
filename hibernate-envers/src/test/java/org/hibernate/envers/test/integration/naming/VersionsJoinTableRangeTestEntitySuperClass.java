package org.hibernate.envers.test.integration.naming;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * Mapped superclass for Audit join table test.
 *
 * @author Erik-Berndt Scheper
 */
@MappedSuperclass
@org.hibernate.envers.Audited
public abstract class VersionsJoinTableRangeTestEntitySuperClass {

	@Id
	@GeneratedValue
	private Integer id;

	private String genericValue;

	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	protected void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the genericValue
	 */
	public String getGenericValue() {
		return genericValue;
	}

	/**
	 * @param genericValue the genericValue to set
	 */
	public void setGenericValue(String genericValue) {
		this.genericValue = genericValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((genericValue == null) ? 0 : genericValue.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		VersionsJoinTableRangeTestEntitySuperClass other = (VersionsJoinTableRangeTestEntitySuperClass) obj;
		if ( genericValue == null ) {
			if ( other.genericValue != null ) {
				return false;
			}
		}
		else if ( !genericValue.equals( other.genericValue ) ) {
			return false;
		}
		if ( id == null ) {
			if ( other.id != null ) {
				return false;
			}
		}
		else if ( !id.equals( other.id ) ) {
			return false;
		}
		return true;
	}

}
